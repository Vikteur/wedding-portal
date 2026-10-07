#!/usr/bin/env bash
# Wait until a human records the review and the STOP approval on the run's pull request (P2: a script, not a model).
#   wait-approval.sh <pull request url> <head sha>
# The approval is a pull request comment or review whose text starts with "Approved", made after the head commit.
# Every comment the run posts comes from the same account, so the word is the mark, not the author; the workflow
# never writes it. The pull request stays a draft until then, so it is approved before it can be merged (DoD #5, #11).
# Prints one JSON line {"approval": "<comment url>", "by": "<login>", "at": "<time>", "sha": "<head sha>"}.
# A pull request merged or closed without one fails the node: nothing is finalized, committed or deleted.
set -euo pipefail
pr=$1 sha=$2
POLL=${APPROVAL_POLL_SECONDS:-60}
repo=$(printf '%s' "$pr" | sed -E 's#^https://github.com/([^/]+/[^/]+)/pull/.*#\1#')
since=$(gh api "repos/$repo/commits/$sha" -q .commit.committer.date)

# A failing gh call (network, rate limit) is retried at the next poll instead of ending a wait that can last days.
while :; do
  view=$(gh pr view "$pr" --json state,comments,reviews 2>/dev/null || echo '{}')
  found=$(printf '%s' "$view" | node -e '
    let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
      const [pr, since] = process.argv.slice(1);
      const v = s.trim() ? JSON.parse(s) : {};
      const items = [
        ...(v.comments || []).map(c => ({ url: c.url, by: c.author.login, at: c.createdAt, body: c.body })),
        ...(v.reviews || []).map(r => ({ url: pr, by: r.author.login, at: r.submittedAt, body: r.body })),
      ];
      const hit = items.find(i => /^\s*approved\b/i.test(i.body || "") && i.at >= since);
      console.log(hit ? JSON.stringify({ approval: hit.url, by: hit.by, at: hit.at }) : (v.state || "UNKNOWN"));
    });' "$pr" "$since")
  case "$found" in
    \{*) printf '%s' "$found" | node -e '
           let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
             console.log(JSON.stringify({ ...JSON.parse(s), sha: process.argv[1] }));
           });' "$sha"
         exit 0 ;;
    MERGED|CLOSED)
      echo "$pr was ${found,,} without an approval comment after $sha: nothing is finalized, committed or deleted." >&2
      exit 1 ;;
  esac
  sleep "$POLL"
done
