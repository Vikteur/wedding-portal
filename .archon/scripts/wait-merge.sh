#!/usr/bin/env bash
# Wait until the user merges or closes the run's pull request (P2: a script, not a model).
#   wait-merge.sh <pull request url>
# Prints one JSON line {"state": "merged|closed", "merge": "...", "head": "...", "branch": "...", "ci": "...",
# "backlog": "..."}: the merge commit, the head SHA and branch of the pull request, the CI state of that head SHA, and
# the umbrella checkout whose backlog/ the ticket is finalized in (BACKLOG_CWD overrides).
# The merge is the human approval for closing the ticket out (UD-21.c). A merge whose head SHA is not green by CI
# fails the node, so nothing is finalized, committed or deleted (P5).
set -euo pipefail
pr=$1
POLL=${MERGE_POLL_SECONDS:-60}

# The Backlog is finalized on the umbrella's main checkout, never in a run worktree: the run's branch is merged.
main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
if [ -z "${BACKLOG_CWD:-}" ]; then
  for dir in "$main" "$main"/../*; do
    if [ -f "$dir/backlog/config.yml" ]; then BACKLOG_CWD=$(cd "$dir" && pwd); break; fi
  done
fi
if [ -z "${BACKLOG_CWD:-}" ]; then
  echo "No backlog/ found beside $main; set BACKLOG_CWD to the umbrella repo." >&2
  exit 1
fi

# A failing gh call (network, rate limit) is retried at the next poll instead of ending a wait that can last days.
while :; do
  state=$(gh pr view "$pr" --json state -q .state 2>/dev/null || echo UNKNOWN)
  case "$state" in MERGED|CLOSED) break ;; esac
  sleep "$POLL"
done

merge=$(gh pr view "$pr" --json mergeCommit -q '.mergeCommit.oid // ""')
head=$(gh pr view "$pr" --json headRefOid -q .headRefOid)
branch=$(gh pr view "$pr" --json headRefName -q .headRefName)
repo=$(printf '%s' "$pr" | sed -E 's#^https://github.com/([^/]+/[^/]+)/pull/.*#\1#')

conclusions=$(gh run list --repo "$repo" --commit "$head" --json conclusion -q '.[].conclusion')
ci=green
if [ -z "$conclusions" ]; then
  ci=none
elif printf '%s\n' "$conclusions" | grep -qvx success; then
  ci=red
fi

lower=$(printf '%s' "$state" | tr '[:upper:]' '[:lower:]')
node -e 'const [state, merge, head, branch, ci, backlog] = process.argv.slice(1);
  console.log(JSON.stringify({ state, merge, head, branch, ci, backlog }));' \
  "$lower" "$merge" "$head" "$branch" "$ci" "$BACKLOG_CWD"

if [ "$state" = MERGED ] && [ "$ci" != green ]; then
  echo "Merged, but CI on head $head is $ci: the ticket is not finalized and nothing is deleted (P5)." >&2
  exit 1
fi
