#!/usr/bin/env bash
# Wait until the user merges or closes the run's pull request (P2: a script, not a model).
#   wait-merge.sh <pull request url> [<ticket>]
# Prints one JSON line {"state": "merged|closed", "merge": "...", "head": "...", "branch": "...", "ci": "...",
# "backlog": "..."}: the merge commit, the head SHA and branch of the pull request, the CI state of that head SHA, and
# the main checkout whose backlog/ the ticket is finalized in: the backlog its ID prefix names (backlog-home.sh), empty
# without a ticket. BACKLOG_CWD names it instead.
# The merge is the human approval for closing the ticket out (UD-21.c). A merge whose head SHA is not green by CI
# fails the node, so nothing is finalized, committed or deleted (P5).
set -euo pipefail
pr=$1 task=${2:-}
POLL=${MERGE_POLL_SECONDS:-60}
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. "$here/backlog-home.sh"

# The ticket is finalized on its backlog's main checkout, never in a run worktree: the run's branch is merged. Found
# before the wait, so a ticket no backlog owns fails now instead of after the merge.
find_ticket "$task"
if [ -n "$task" ] && [ -z "$backlog_home" ]; then
  echo "No backlog beside this repo uses the prefix of $task; set BACKLOG_CWD to the repo that holds it." >&2
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
  "$lower" "$merge" "$head" "$branch" "$ci" "$backlog_home"

if [ "$state" = MERGED ] && [ "$ci" != green ]; then
  echo "Merged, but CI on head $head is $ci: the ticket is not finalized and nothing is deleted (P5)." >&2
  exit 1
fi
