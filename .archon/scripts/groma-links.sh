#!/usr/bin/env bash
# The ticket's links to the Groma map in build-feature (TASK-36; P2: a script, not a model). In a repo with a groma/
# map, adds the branch's changed files to the Backlog ticket's modified files, and the map elements that own them
# (`groma view <file>`) to its references, keeping what the ticket already holds. Groma then shows the ticket on the
# map while it is To Do or In Progress. The ticket is edited in the umbrella checkout and committed by close-out.
# A run without a ticket, or a repo without groma/, links nothing. GROMA names the CLI (default: groma).
#   groma-links.sh "<TASK id or empty>" <base-branch>    prints {"files": <changed files>, "refs": "<owner ids>"}
set -euo pipefail
task=${1:-}
base=${2:?usage: groma-links.sh <TASK id or empty> <base-branch>}
groma=${GROMA:-groma}

if [ -z "$task" ] || [ ! -f groma/scanners.json ]; then
  echo '{"files": 0, "refs": ""}'
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
refs=()
for f in "${files[@]}"; do
  owner=$("$groma" view "$f" --plain 2>/dev/null | awk 'prev == "-----" && seen { print $1; exit } $0 == "Owner" { seen = 1 } { prev = $0 }') || owner=""
  if [ -n "$owner" ] && [[ ! " ${refs[*]} " == *" $owner "* ]]; then refs+=("$owner"); fi
done

# --modified-file replaces the list, so the ticket's files go first; --add-ref adds, so only new references are passed.
mapfile -t have < <(backlog task view "$task" --json | node -e '
  let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
    const t = JSON.parse(s).task;
    for (const f of t.modifiedFiles || []) console.log("file " + f);
    for (const r of t.references || []) console.log("ref " + r);
  });')
old_files=(); old_refs=()
for line in "${have[@]}"; do
  case "$line" in "file "*) old_files+=("${line#file }") ;; "ref "*) old_refs+=("${line#ref }") ;; esac
done

args=(); new=0
for f in "${old_files[@]}"; do args+=(--modified-file "$f"); done
for f in "${files[@]}"; do
  if [[ ! " ${old_files[*]} " == *" $f "* ]]; then args+=(--modified-file "$f"); new=1; fi
done
for r in "${refs[@]}"; do
  if [[ ! " ${old_refs[*]} " == *" $r "* ]]; then args+=(--add-ref "$r"); new=1; fi
done
if [ "$new" = 1 ]; then
  backlog task edit "$task" "${args[@]}" > /dev/null
fi

joined=$(IFS=,; echo "${refs[*]}")
echo "{\"files\": ${#files[@]}, \"refs\": \"$joined\"}"
