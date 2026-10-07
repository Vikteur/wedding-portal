#!/usr/bin/env bash
# Gather the evidence a retro is written from (P2: a script, not a model).
#   retro-evidence.sh <request> <run id> <artifacts dir> <base branch>
# Runs after every build-feature run, merged or not, in the run's worktree. It reads no output of another node, because
# any of them may have been skipped: the ticket comes from the request, the branch from git, the pull request from gh.
# Writes <artifacts dir>/retro-evidence.md (node timeline, the agents' last messages, commits, the pull request with its
# reviews and comments, CI runs, artifacts) and prints one JSON line
#   {"umbrella": "...", "dir": "docs/retro/<TASK or run-<id8>>", "evidence": "...", "next_adr": "NN", "task": "..."}
# A failing gh or archon call is written down as unavailable instead of failing the run; no umbrella found fails it.
# BACKLOG_CWD names the umbrella; RETRO_TRANSCRIPT a transcript file to read instead of `archon workflow logs`.
set -uo pipefail
request=$1 run=$2 art=$3 base=${4:-main}
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# The retro is committed on the umbrella's main checkout, never in a run worktree (same search as wait-merge.sh).
main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
umbrella=${BACKLOG_CWD:-}
if [ -z "$umbrella" ]; then
  for dir in "$main" "$main"/../*; do
    if [ -f "$dir/backlog/config.yml" ]; then umbrella=$(cd "$dir" && pwd); break; fi
  done
fi
if [ -z "$umbrella" ]; then
  echo "No backlog/ found beside $main; set BACKLOG_CWD to the umbrella repo." >&2
  exit 1
fi
# A POSIX path: the workflow assigns this output unquoted, which would eat the backslashes of C:\...
umbrella=$(cd "$umbrella" && pwd) || exit 1

task=$(printf '%s' "$request" | grep -oiE 'task-[0-9]+(\.[0-9]+)*' | head -1 | tr '[:lower:]' '[:upper:]')
dir="docs/retro/${task:-run-${run:0:8}}"
last=$(ls "$umbrella/$dir/adr" 2>/dev/null | sed -nE 's/^ADR-([0-9]+)-.*\.md$/\1/p' | sort -n | tail -1)
next_adr=$(printf '%02d' $((10#${last:-0} + 1)))

work="$art/retro"
mkdir -p "$work"
branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")

if [ -n "${RETRO_TRANSCRIPT:-}" ]; then cp "$RETRO_TRANSCRIPT" "$work/transcript.jsonl"
else archon workflow logs "$run" > "$work/transcript.jsonl" 2>/dev/null || : > "$work/transcript.jsonl"; fi

pr=$(gh pr list --head "$branch" --state all --json url -q '.[0].url' 2>/dev/null | head -1 || true)
: > "$work/pr.json"
[ -n "$pr" ] && { gh pr view "$pr" \
  --json number,title,state,mergedAt,mergeCommit,headRefOid,headRefName,commits,reviews,comments \
  > "$work/pr.json" 2>/dev/null || : > "$work/pr.json"; }
gh run list --branch "$branch" --limit 30 --json databaseId,headSha,conclusion,status,workflowName,createdAt \
  > "$work/runs.json" 2>/dev/null || : > "$work/runs.json"

from=$(git rev-parse -q --verify "origin/$base" || git rev-parse -q --verify "$base" || true)
git log --format='%h %s' ${from:+"$from..HEAD"} > "$work/commits.txt" 2>/dev/null || : > "$work/commits.txt"
(cd "$art" && find . -type f ! -path './retro/*' ! -name retro-evidence.md | sed 's#^\./##' | sort) > "$work/artifacts.txt"

evidence="$art/retro-evidence.md"
node -e 'const [request, run, task, branch, pr, dir, next_adr, w, artifacts_dir, out] = process.argv.slice(1);
  require("fs").writeFileSync(out, JSON.stringify({ request, run, task, branch, pr, dir, next_adr, artifacts_dir,
    transcript: w + "/transcript.jsonl", pr_json: w + "/pr.json", runs_json: w + "/runs.json",
    commits: w + "/commits.txt", artifacts: w + "/artifacts.txt" }));' \
  "$request" "$run" "$task" "$branch" "$pr" "$dir" "$next_adr" "$work" "$art" "$work/meta.json"
node "$here/retro.js" render "$work/meta.json" "$evidence" || exit 1

node -e 'const [umbrella, dir, evidence, next_adr, task] = process.argv.slice(1);
  console.log(JSON.stringify({ umbrella, dir, evidence, next_adr, task }));' \
  "$umbrella" "$dir" "$evidence" "$next_adr" "$task"
