#!/usr/bin/env bash
# Refresh the generated block of a pull request body so it matches the branch as it is now (retros found PR bodies
# stale after fix rounds: wrong test counts, wrong claims). Only the block between the two markers is rewritten; the
# rest of the body is never touched.
#   pr-refresh.sh <pr> <base> <body-file>
#     <body-file> is rewritten in place, then `gh pr edit <pr> --body-file <body-file>` publishes it.
#     The block holds: the commits of origin/<base>..HEAD, the changed-file count, and, when
#     $ARTIFACTS_DIR/plan.md has a "## Owner decisions" section, a copy of that section's body.
#     Without a block the new one goes before the final "Generated with" line (or at the end). Idempotent.
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "usage: pr-refresh.sh <pr> <base> <body-file>" >&2; exit 2
fi
pr=$1; base=$2; body=$3
[ -f "$body" ] || { echo "pr-refresh: no such body file: $body" >&2; exit 2; }

start='<!-- pr-refresh:start -->'
end='<!-- pr-refresh:end -->'
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

files=$(git diff --name-only "origin/$base...HEAD" | wc -l | tr -d ' ')
{
  echo "$start"
  echo "### Commits (generated)"
  echo
  git log --format='- %h %s' "origin/$base..HEAD"
  echo
  if [ "$files" = 1 ]; then echo "1 file changed"; else echo "$files files changed"; fi
  plan="${ARTIFACTS_DIR:-}/plan.md"
  if [ -n "${ARTIFACTS_DIR:-}" ] && [ -f "$plan" ] && grep -q '^## Owner decisions[[:space:]]*$' "$plan"; then
    echo
    echo "### Owner decisions (from the plan)"
    awk '/^## /{ if (on) exit; on = ($0 ~ /^## Owner decisions[ \t]*$/); next } on' "$plan"
  fi
  echo "$end"
} > "$tmp/block"

if grep -qxF "$start" "$body"; then
  # Replace in place: everything between the markers (markers included) becomes the new block.
  awk -v bf="$tmp/block" -v s="$start" -v e="$end" '
    $0 == s { while ((getline line < bf) > 0) print line; skip = 1; next }
    skip && $0 == e { skip = 0; next }
    !skip { print }' "$body" > "$tmp/new"
elif grep -q 'Generated with' "$body"; then
  last=$(grep -n 'Generated with' "$body" | tail -n 1 | cut -d: -f1)
  { head -n $((last - 1)) "$body"; cat "$tmp/block"; echo; tail -n +"$last" "$body"; } > "$tmp/new"
else
  { cat "$body"; echo; cat "$tmp/block"; } > "$tmp/new"
fi

cat "$tmp/new" > "$body"
gh pr edit "$pr" --body-file "$body"
