#!/usr/bin/env bash
# Gather the evidence a retro is written from (P2: a script, not a model).
#   retro-evidence.sh <request> <run id> <artifacts dir> <base branch> [<pull request URL>]
# Runs after the pull request is merged, from the umbrella's main checkout: the run's branch and worktree are gone by
# then, so the pull request is given and everything (branch, commits, head SHA for CI) is read from it. It reads no
# output of another node, because any of them may have been skipped: the ticket comes from the request. Without the
# pull request it falls back to the current branch (git rev-parse, gh pr list --head, git log <base>..HEAD).
# Writes <artifacts dir>/retro-evidence.md (node timeline, the agents' last messages, commits, the pull request with its
# reviews and comments, CI runs, artifacts) and prints one JSON line
#   {"umbrella": "...", "dir": "docs/retro/<TASK or run-<id8>>", "evidence": "...", "next_adr": "NN", "task": "..."}
# A failing gh or archon call is written down as unavailable instead of failing the run; no umbrella found fails it.
# RETRO_HOME names the umbrella (retro-umbrella.sh); RETRO_TRANSCRIPT a transcript file to read instead of `archon workflow logs`.
set -uo pipefail
request=$1 run=$2 art=$3 base=${4:-main} pr_arg=${5:-}
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

. "$here/retro-umbrella.sh"
find_umbrella || exit 1

task=$(request_ticket "$request")   # TASK-n, TOOL-n: the prefix of any backlog beside the umbrella
dir="docs/retro/${task:-run-${run:0:8}}"
last=$(ls "$umbrella/$dir/adr" 2>/dev/null | sed -nE 's/^ADR-([0-9]+)-.*\.md$/\1/p' | sort -n | tail -1)
next_adr=$(printf '%02d' $((10#${last:-0} + 1)))

work="$art/retro"
mkdir -p "$work"

if [ -n "${RETRO_TRANSCRIPT:-}" ]; then cp "$RETRO_TRANSCRIPT" "$work/transcript.jsonl"
else archon workflow logs "$run" > "$work/transcript.jsonl" 2>/dev/null || : > "$work/transcript.jsonl"; fi

json_field() {
  node -e 'try { const v = JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"))[process.argv[2]];
    if (v) console.log(v); } catch (e) {}' "$1" "$2"
}
runs_json=databaseId,headSha,conclusion,status,workflowName,createdAt
: > "$work/pr.json"
if [ -n "$pr_arg" ]; then
  gh pr view "$pr_arg" \
    --json number,title,state,mergedAt,mergeCommit,headRefOid,headRefName,commits,reviews,comments,url \
    > "$work/pr.json" 2>/dev/null || : > "$work/pr.json"
  branch=$(json_field "$work/pr.json" headRefName)
  head=$(json_field "$work/pr.json" headRefOid)
  pr=$(json_field "$work/pr.json" url); pr=${pr:-$pr_arg}
  # The pull request may live in another repo than the umbrella this runs in: ask that repo for its CI runs.
  slug=$(printf '%s' "$pr" | sed -nE 's#^https?://[^/]+/([^/]+/[^/]+)/pull/[0-9]+.*#\1#p')
  : > "$work/runs.json"
  { [ -n "$head" ] && gh run list ${slug:+-R "$slug"} --commit "$head" --limit 30 --json $runs_json \
      > "$work/runs.json" 2>/dev/null; } ||
    { [ -n "$branch" ] && gh run list ${slug:+-R "$slug"} --branch "$branch" --limit 30 --json $runs_json \
      > "$work/runs.json" 2>/dev/null; } || : > "$work/runs.json"
  node -e 'try { for (const c of JSON.parse(require("fs").readFileSync(process.argv[1], "utf8")).commits || [])
    console.log(c.oid.slice(0, 7) + " " + c.messageHeadline); } catch (e) {}' \
    "$work/pr.json" > "$work/commits.txt" 2>/dev/null || : > "$work/commits.txt"
else
  branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  pr=$(gh pr list --head "$branch" --state all --json url -q '.[0].url' 2>/dev/null | head -1 || true)
  [ -n "$pr" ] && { gh pr view "$pr" \
    --json number,title,state,mergedAt,mergeCommit,headRefOid,headRefName,commits,reviews,comments \
    > "$work/pr.json" 2>/dev/null || : > "$work/pr.json"; }
  gh run list --branch "$branch" --limit 30 --json $runs_json \
    > "$work/runs.json" 2>/dev/null || : > "$work/runs.json"
  from=$(git rev-parse -q --verify "origin/$base" || git rev-parse -q --verify "$base" || true)
  git log --format='%h %s' ${from:+"$from..HEAD"} > "$work/commits.txt" 2>/dev/null || : > "$work/commits.txt"
fi
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
