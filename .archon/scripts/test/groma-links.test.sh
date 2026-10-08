#!/usr/bin/env bash
# Tests for .archon/scripts/groma-links.sh against throwaway repos and stubbed groma and backlog CLIs.
# Usage: bash .archon/scripts/test/groma-links.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/groma-links.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

# The groma stub owns src/a/* (comp-a) and src/b/* (comp-b), like `groma view <file> --plain`; any other file has no
# owner, and a file that is gone is an unknown target. Both of those exit 1, like the real CLI.
mkdir -p "$root/bin"
cat > "$root/bin/groma" <<'STUB'
#!/usr/bin/env bash
echo "groma $*" >> "$GROMA_LOG"
[ "$1" = view ] || exit 0
[ -e "$2" ] || { echo "unknown target: $2; not a repository file"; exit 1; }
case "$2" in
  src/a/*) printf 'Owner\n-----\ncomp-a  component  A\nparent: api\n' ;;
  src/b/*) printf 'Owner\n-----\ncomp-b  component  B\nparent: api\n' ;;
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
check "L1 nothing linked" [ "$out" = '{"files": 0, "refs": ""}' ]
check "L1 groma not called" [ ! -s "$log" ]
check "L1 backlog not called" [ ! -s "$blog" ]

# L2: a repo without a map links nothing.
repo l2
change src/a/A.java
run TASK-9
check "L2 exit 0" [ "$rc" = 0 ]
check "L2 nothing linked" [ "$out" = '{"files": 0, "refs": ""}' ]
check "L2 backlog not called" [ ! -s "$blog" ]

# L3: the branch's changed files all go to the ticket, and the components that own them become references. A file
# without an owner, or one the branch deleted, is still a changed file, but gives no reference.
repo l3 map
change src/a/A.java src/b/B.java docs/notes.md
git -C "$dir" rm -q src/a/Old.java; git -C "$dir" commit -qm delete
run TASK-9
check "L3 exit 0" [ "$rc" = 0 ]
check "L3 output" [ "$out" = '{"files": 4, "refs": "comp-a,comp-b"}' ]
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
check "L5 output" [ "$out" = '{"files": 1, "refs": "comp-a"}' ]

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

echo "groma-links tests: $pass passed, $fail failed"
[ "$fail" = 0 ]
