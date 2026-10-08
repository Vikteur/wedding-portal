#!/usr/bin/env bash
# Tests for .archon/scripts/groma-links.sh against throwaway repos and stubbed groma and backlog CLIs.
# Usage: bash .archon/scripts/test/groma-links.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/groma-links.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

# The groma stub owns src/a/* (comp-a) and src/b/* (comp-b), like `groma view <file> --plain`; any other file has no
# owner, and a file that is gone is an unknown target. Both of those exit 1, like the real CLI. The map holds
#   system sys > containers api, cont-x, other-cont > components comp-a, comp-b, outer (> inner), comp-z
# and `groma view <id> --plain` answers an element block for an id, like the real CLI; an unknown id exits 1.
#   src/n/* is owned by inner (under outer under cont-x), src/c/* by the container api itself, src/z/* by comp-z.
mkdir -p "$root/bin"
cat > "$root/bin/groma" <<'STUB'
#!/usr/bin/env bash
echo "groma $*" >> "$GROMA_LOG"
[ "$1" = view ] || exit 0
element() { # element <id> <type> <title> [parent]
  printf 'Element\n-------\n%s  %s  %s\n' "$1" "$2" "$3"
  [ -z "${4:-}" ] || printf 'parent: %s\n' "$4"
  printf 'technology: Java 25\n'
}
if [ ! -e "$2" ]; then
  case "$2" in
    comp-a) element comp-a component A api ;;
    comp-b) element comp-b component B api ;;
    comp-z) element comp-z component Z other-cont ;;
    inner) element inner component Inner outer ;;
    outer) element outer component Outer cont-x ;;
    api) element api container API sys ;;
    cont-x) element cont-x container X sys ;;
    other-cont) element other-cont container Other sys ;;
    sys) element sys system System ;;
    *) echo "unknown target: $2; not a repository file"; exit 1 ;;
  esac
  exit 0
fi
case "$2" in
  src/a/*) printf 'Owner\n-----\ncomp-a  component  A\nparent: api\n' ;;
  src/b/*) printf 'Owner\n-----\ncomp-b  component  B\nparent: api\n' ;;
  src/n/*) printf 'Owner\n-----\ninner  component  Inner\nparent: outer\n' ;;
  src/c/*) printf 'Owner\n-----\napi  container  API\nparent: sys\n' ;;
  src/z/*) printf 'Owner\n-----\ncomp-z  component  Z\nparent: other-cont\n' ;;
  *) echo "no owner: $2; no enabled scanner reads it"; exit 1 ;;
esac
STUB
# The backlog stub answers `task view <id> --json` from $BACKLOG_STATE and logs every call with its BACKLOG_CWD.
cat > "$root/bin/backlog" <<'STUB'
#!/usr/bin/env bash
echo "cwd=${BACKLOG_CWD:-} backlog $*" >> "$BACKLOG_LOG"
[ "$1 $2" = "task view" ] && cat "$BACKLOG_STATE"
exit 0
STUB
chmod +x "$root/bin/groma" "$root/bin/backlog"

# repo <name> [map]: a repo whose main has one commit, then a branch `work`; with a groma/ map when map is given.
# Sets dir, log (groma calls), blog (backlog calls) and state (the ticket's JSON: no files, no references).
repo() {
  dir="$root/$1"; log="$root/$1.groma.log"; blog="$root/$1.backlog.log"; state="$root/$1.json"
  mkdir -p "$(dirname "$dir")"; : > "$log"; : > "$blog"
  echo '{"task": {"id": "TASK-9", "modifiedFiles": [], "references": []}}' > "$state"
  git init -q -b main "$dir"
  git -C "$dir" config user.name t; git -C "$dir" config user.email t@t; git -C "$dir" config core.autocrlf false
  mkdir -p "$dir/src/a"
  echo old > "$dir/src/a/Old.java"
  echo base > "$dir/README.md"
  if [ -n "${2:-}" ]; then
    mkdir -p "$dir/groma"
    echo '{"scanners": [{"id": "java"}]}' > "$dir/groma/scanners.json"
  fi
  git -C "$dir" add -A; git -C "$dir" commit -qm base
  git -C "$dir" checkout -qb work
}
change() { # change <path>...: commit a new version of each file on the current branch
  for f in "$@"; do mkdir -p "$dir/$(dirname "$f")"; echo "$RANDOM" >> "$dir/$f"; done
  git -C "$dir" add -A; git -C "$dir" commit -qm change
}
run() { # run <task> [env...]: groma-links inside the repo against base main; sets out (stdout), err, rc
  local task=$1; shift
  out=$( (cd "$dir" && env PATH="$root/bin:$PATH" GROMA_LOG="$log" BACKLOG_LOG="$blog" BACKLOG_STATE="$state" \
    BACKLOG_CWD="$root/umbrella" "$@" bash "$script" "$task" main) 2>"$root/err" ); rc=$?
  err=$(cat "$root/err")
}
check() { # check <name> <condition...>
  local name=$1; shift
  if "$@"; then pass=$((pass + 1)); else fail=$((fail + 1)); echo "FAIL: $name"; echo "  out: $out"; echo "  err: $err"; fi
}
edits() { grep -c "backlog task edit" "$blog"; }
edit_line() { grep "backlog task edit" "$blog"; }

# L1: a run without a ticket links nothing and calls neither CLI.
repo l1 map
change src/a/A.java
run ""
check "L1 exit 0" [ "$rc" = 0 ]
check "L1 nothing linked" [ "$out" = '{"files": 0, "refs": "", "dropped": ""}' ]
check "L1 groma not called" [ ! -s "$log" ]
check "L1 backlog not called" [ ! -s "$blog" ]

# L2: a repo without a map links nothing.
repo l2
change src/a/A.java
run TASK-9
check "L2 exit 0" [ "$rc" = 0 ]
check "L2 nothing linked" [ "$out" = '{"files": 0, "refs": "", "dropped": ""}' ]
check "L2 backlog not called" [ ! -s "$blog" ]

# L3: the branch's changed files all go to the ticket, and the components that own them become references. A file
# without an owner, or one the branch deleted, is still a changed file, but gives no reference.
repo l3 map
change src/a/A.java src/b/B.java docs/notes.md
git -C "$dir" rm -q src/a/Old.java; git -C "$dir" commit -qm delete
run TASK-9
check "L3 exit 0" [ "$rc" = 0 ]
check "L3 output" [ "$out" = '{"files": 4, "refs": "comp-a,comp-b", "dropped": ""}' ]
check "L3 one edit" [ "$(edits)" = 1 ]
check "L3 edit names the ticket" grep -q "backlog task edit TASK-9 " "$blog"
check "L3 every changed file" [ "$(edit_line | grep -o -- '--modified-file [^ ]*' | tr '\n' ' ')" = \
  "--modified-file docs/notes.md --modified-file src/a/A.java --modified-file src/a/Old.java --modified-file src/b/B.java " ]
check "L3 owners as references" [ "$(edit_line | grep -o -- '--add-ref [^ ]*' | tr '\n' ' ')" = "--add-ref comp-a --add-ref comp-b " ]
check "L3 backlog in the umbrella" grep -q "^cwd=$root/umbrella backlog task edit" "$blog"

# L4: what the ticket already holds is kept, first and once: the flags replace the list, so it is passed again.
repo l4 map
change src/a/A.java src/b/B.java
echo '{"task": {"id": "TASK-9", "modifiedFiles": ["planned/X.java", "src/a/A.java"], "references": ["comp-a", "wedding-portal-api"]}}' > "$state"
run TASK-9
check "L4 exit 0" [ "$rc" = 0 ]
check "L4 old files first, no duplicate" [ "$(edit_line | grep -o -- '--modified-file [^ ]*' | tr '\n' ' ')" = \
  "--modified-file planned/X.java --modified-file src/a/A.java --modified-file src/b/B.java " ]
check "L4 only the new reference" [ "$(edit_line | grep -o -- '--add-ref [^ ]*' | tr '\n' ' ')" = "--add-ref comp-b " ]

# L5: a ticket that already holds every file and reference is not edited.
repo l5 map
change src/a/A.java
echo '{"task": {"id": "TASK-9", "modifiedFiles": ["src/a/A.java"], "references": ["comp-a"]}}' > "$state"
run TASK-9
check "L5 exit 0" [ "$rc" = 0 ]
check "L5 no edit" [ "$(edits)" = 0 ]
check "L5 output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": ""}' ]

# L6: commits on main after the branch point are not the branch's changes.
repo l6 map
change src/a/A.java
git -C "$dir" checkout -q main; change src/b/B.java; git -C "$dir" checkout -q work
run TASK-9
check "L6 exit 0" [ "$rc" = 0 ]
check "L6 only the branch's file" [ "$(edit_line | grep -o -- '--modified-file [^ ]*' | tr '\n' ' ')" = "--modified-file src/a/A.java " ]

# L7: without BACKLOG_CWD the backlog is found in the umbrella checkout beside the repo, like ticket.sh finds it.
mkdir -p "$root/side/weddingapp/backlog"; touch "$root/side/weddingapp/backlog/config.yml"
repo side/portal map
change src/a/A.java
run TASK-9 BACKLOG_CWD=
check "L7 exit 0" [ "$rc" = 0 ]
check "L7 backlog beside the repo" grep -q "^cwd=$root/side/weddingapp backlog task edit" "$blog"

# L8: no backlog anywhere fails, and edits nothing.
repo lone/portal map
change src/a/A.java
run TASK-9 BACKLOG_CWD=
check "L8 exit 1" [ "$rc" = 1 ]
check "L8 says so" grep -q "No backlog/ found for TASK-9" "$root/err"
check "L8 no edit" [ "$(edits)" = 0 ]

# L9: a map but no groma CLI fails with the install hint.
repo l9 map
change src/a/A.java
run TASK-9 GROMA=groma-not-installed
check "L9 exit 1" [ "$rc" = 1 ]
check "L9 install hint" grep -q "npm install -g groma.md" "$root/err"
check "L9 no edit" [ "$(edits)" = 0 ]

# L10: stdout is the JSON alone.
repo l10 map
change src/a/A.java docs/notes.md
run TASK-9
check "L10 stdout is JSON" node -e 'JSON.parse(process.argv[1])' "$out"

# A ticket planned on a container gets a component reference once a component under it is linked; the container
# reference then goes in the same edit (--remove-ref). state_refs <refs...> sets the ticket's references.
state_refs() { # state_refs <ref>...: the ticket holds no files and these references
  local joined; joined=$(printf '"%s",' "$@"); echo "{\"task\": {\"id\": \"TASK-9\", \"modifiedFiles\": [], \"references\": [${joined%,}]}}" > "$state"
}
removed() { edit_line | grep -o -- '--remove-ref [^ ]*' | tr '\n' ' '; }

# L11: the container directly above a newly linked component is dropped in the same edit.
repo l11 map
change src/a/A.java
state_refs api
run TASK-9
check "L11 exit 0" [ "$rc" = 0 ]
check "L11 one edit" [ "$(edits)" = 1 ]
check "L11 component added" [ "$(edit_line | grep -o -- '--add-ref [^ ]*' | tr '\n' ' ')" = "--add-ref comp-a " ]
check "L11 container removed" [ "$(removed)" = "--remove-ref api " ]
check "L11 output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": "api"}' ]

# L12: the container two levels up (component under component under container) is dropped too.
repo l12 map
change src/n/N.java
state_refs cont-x
run TASK-9
check "L12 exit 0" [ "$rc" = 0 ]
check "L12 container removed" [ "$(removed)" = "--remove-ref cont-x " ]
check "L12 output" [ "$out" = '{"files": 1, "refs": "inner", "dropped": "cont-x"}' ]

# L13: a container none of whose components own a changed file stays.
repo l13 map
change src/a/A.java
state_refs other-cont
run TASK-9
check "L13 exit 0" [ "$rc" = 0 ]
check "L13 component added" [ "$(edit_line | grep -o -- '--add-ref [^ ]*' | tr '\n' ' ')" = "--add-ref comp-a " ]
check "L13 nothing removed" [ -z "$(removed)" ]
check "L13 output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": ""}' ]

# L14: a container that owns a changed file itself stays, even beside a component under it.
repo l14 map
change src/a/A.java src/c/C.java
state_refs api
run TASK-9
check "L14 exit 0" [ "$rc" = 0 ]
check "L14 nothing removed" [ -z "$(removed)" ]
check "L14 output" [ "$out" = '{"files": 2, "refs": "comp-a,api", "dropped": ""}' ]

# L15: references that are not map elements (file paths, URLs) are never removed, nor is a system.
repo l15 map
change src/a/A.java
state_refs "rekord-api/Dockerfile:31-37" "https://example.org/spec" sys api
run TASK-9
check "L15 exit 0" [ "$rc" = 0 ]
check "L15 only the container removed" [ "$(removed)" = "--remove-ref api " ]
check "L15 output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": "api"}' ]

# L16: a system reference above the owner is kept when the ticket holds no container.
repo l16 map
change src/a/A.java
state_refs sys
run TASK-9
check "L16 exit 0" [ "$rc" = 0 ]
check "L16 nothing removed" [ -z "$(removed)" ]
check "L16 system kept in the output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": ""}' ]

# L17: two components under the same container drop it once; an owner's parents are looked up once each.
repo l17 map
change src/a/A.java src/b/B.java
state_refs api
run TASK-9
check "L17 container removed once" [ "$(removed)" = "--remove-ref api " ]
check "L17 each element looked up once" [ "$(grep -c 'groma view api --plain' "$log")" = 1 ]

# L18: nothing new and no container to drop: no edit, and nothing dropped.
repo l18 map
change src/a/A.java
echo '{"task": {"id": "TASK-9", "modifiedFiles": ["src/a/A.java"], "references": ["comp-a", "other-cont", "rekord-api/Dockerfile:31-37"]}}' > "$state"
run TASK-9
check "L18 exit 0" [ "$rc" = 0 ]
check "L18 no edit" [ "$(edits)" = 0 ]
check "L18 output" [ "$out" = '{"files": 1, "refs": "comp-a", "dropped": ""}' ]

# L19: a container is dropped even when the component is already linked and nothing else is new.
repo l19 map
change src/a/A.java
echo '{"task": {"id": "TASK-9", "modifiedFiles": ["src/a/A.java"], "references": ["comp-a", "api"]}}' > "$state"
run TASK-9
check "L19 edits for the removal alone" [ "$(edits)" = 1 ]
check "L19 container removed" [ "$(removed)" = "--remove-ref api " ]
check "L19 nothing added" [ -z "$(edit_line | grep -o -- '--add-ref [^ ]*')" ]

echo "groma-links tests: $pass passed, $fail failed"
[ "$fail" = 0 ]
