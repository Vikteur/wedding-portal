#!/usr/bin/env bash
# The ticket's links to the Groma map in build-feature (TASK-36; P2: a script, not a model). In a repo with a groma/
# map, adds the branch's changed files to the Backlog ticket's modified files, and the map elements that own them
# (`groma view <file>`) to its references, keeping what the ticket already holds. Groma then shows the ticket on the
# map while it is To Do or In Progress. The ticket is edited in the umbrella checkout and committed by close-out.
# A run without a ticket, or a repo without groma/, links nothing. GROMA names the CLI (default: groma).
# A planned ticket references a container while no component exists; once the ticket gains a component under that
# container (directly or further up the parent chain), the container reference is removed in the same edit. It stays
# when no owner lies under it or when it owns a changed file itself. References that are not map elements (file paths,
# URLs) and systems are never removed.
#   groma-links.sh "<TASK id or empty>" <base-branch>
#     prints {"files": <changed files>, "refs": "<owner ids>", "dropped": "<removed container ids>"}
set -euo pipefail
task=${1:-}
base=${2:?usage: groma-links.sh <TASK id or empty> <base-branch>}
groma=${GROMA:-groma}

if [ -z "$task" ] || [ ! -f groma/scanners.json ]; then
  echo '{"files": 0, "refs": "", "dropped": ""}'
  exit 0
fi
if ! command -v "$groma" > /dev/null; then
  echo "groma/ holds a Groma map but the groma CLI is missing: npm install -g groma.md" >&2
  exit 1
fi

# The Backlog lives in the umbrella checkout, beside the repo's main checkout (as ticket.sh finds it).
if [ -z "${BACKLOG_CWD:-}" ]; then
  main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
  for dir in "$PWD" "$main" "$main"/../*; do
    if [ -f "$dir/backlog/config.yml" ]; then BACKLOG_CWD=$(cd "$dir" && pwd); break; fi
  done
fi
if [ -z "${BACKLOG_CWD:-}" ]; then
  echo "No backlog/ found for $task; set BACKLOG_CWD to the umbrella repo." >&2
  exit 1
fi
export BACKLOG_CWD

from=$(git merge-base HEAD "origin/$base" 2>/dev/null || git merge-base HEAD "$base")
mapfile -t files < <(git diff --name-only "$from" HEAD)

# The owner is the first word under "Owner" in `groma view <file> --plain`; a file without one makes groma exit 1.
# Both `Owner` and `Element` blocks read "<id>  <type>  <title>" under the dashes, with an optional "parent: <id>".
# Only the block's own lines count: a `parent:` after the first blank line belongs to a later block.
block() { tr -d '\015' | awk -v h="$1" '$0 == h { seen = 1; next } seen && prev ~ /^-+$/ && !got { id = $1; type = $2; got = 1 }
  got && /^$/ { exit } got && /^parent: / && !par { par = $2 } { prev = $0 } END { if (got) print id, type, par }'; }
declare -A etype=() eparent=()
lookup() { # lookup <id>: fills etype/eparent from `groma view <id> --plain`, once per id
  [ -z "${etype[$1]+x}" ] || return 0
  local id type parent
  read -r id type parent < <("$groma" view "$1" --plain 2>/dev/null | block Element) || true
  [ -n "${type:-}" ] || echo "groma-links: cannot resolve map element $1; container references above it are kept" >&2
  etype[$1]=${type:-}; eparent[$1]=${parent:-}
}

refs=()
for f in "${files[@]}"; do
  read -r owner type parent < <("$groma" view "$f" --plain 2>/dev/null | block Owner) || owner=""
  if [ -n "$owner" ] && [[ ! " ${refs[*]} " == *" $owner "* ]]; then
    refs+=("$owner"); etype[$owner]=$type; eparent[$owner]=${parent:-}
  fi
done

# One edit: --modified-file replaces the list, so the ticket's files go first; --add-ref adds, so only new references
# are passed; --remove-ref drops the superseded containers.
# The view is read into a variable first: inside `mapfile < <(...)` a failing `backlog task view` would be invisible, and
# the edit would then replace the ticket's file list with this branch's files alone.
view=$(backlog task view "$task" --json | node -e '
  let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
    const t = JSON.parse(s).task;
    for (const f of t.modifiedFiles || []) console.log("file " + f);
    for (const r of t.references || []) console.log("ref " + r);
  });') || { echo "groma-links: cannot read $task from the backlog (backlog task view failed); the ticket is left as it is" >&2; exit 1; }
have=()
[ -z "$view" ] || mapfile -t have <<< "$view"
old_files=(); old_refs=()
for line in ${have[@]+"${have[@]}"}; do
  case "$line" in "file "*) old_files+=("${line#file }") ;; "ref "*) old_refs+=("${line#ref }") ;; esac
done

# The containers above the owners: a ticket reference to one of them (not itself an owner) is superseded.
declare -A above=()
if [ "${#old_refs[@]}" -gt 0 ]; then
  for owner in "${refs[@]}"; do
    id=${eparent[$owner]}; depth=0
    while [ -n "$id" ] && [ "$depth" -lt 20 ]; do  # cycle guard: a loop in the parents ends at depth 20
      lookup "$id"
      if [ "${etype[$id]}" = container ]; then above[$id]=1; fi
      id=${eparent[$id]}; depth=$((depth + 1))
    done
  done
fi
dropped=()
for r in "${old_refs[@]}"; do
  if [ -n "${above[$r]+x}" ] && [[ ! " ${refs[*]} " == *" $r "* ]]; then dropped+=("$r"); fi
done

args=(); new=0
for f in "${old_files[@]}"; do args+=(--modified-file "$f"); done
for f in "${files[@]}"; do
  if [[ ! " ${old_files[*]} " == *" $f "* ]]; then args+=(--modified-file "$f"); new=1; fi
done
for r in "${refs[@]}"; do
  if [[ ! " ${old_refs[*]} " == *" $r "* ]]; then args+=(--add-ref "$r"); new=1; fi
done
for r in "${dropped[@]}"; do args+=(--remove-ref "$r"); new=1; done
if [ "$new" = 1 ]; then
  backlog task edit "$task" "${args[@]}" > /dev/null
fi

joined=$(IFS=,; echo "${refs[*]}")
gone=$(IFS=,; echo "${dropped[*]}")
echo "{\"files\": ${#files[@]}, \"refs\": \"$joined\", \"dropped\": \"$gone\"}"
