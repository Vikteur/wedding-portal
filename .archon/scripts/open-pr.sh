#!/usr/bin/env bash
# Push the run's branch and open a DRAFT pull request against the base branch (P2: a script, not a model).
#   open-pr.sh <base branch> <title> <body file>
# Refuses a protected branch and a dirty work tree. Reuses an open pull request for the branch. The pull request stays
# a draft until ci-by-sha.sh reports the pushed commit green; the ready node then marks it ready for review.
set -euo pipefail
base=$1 title=$2 body=$3

branch=$(git rev-parse --abbrev-ref HEAD)
case "$branch" in
  main|master|HEAD|"$base") echo "Refusing to push '$branch': runs push only their own feature branch." >&2; exit 1 ;;
esac
if [ -n "$(git status --porcelain)" ]; then
  echo "Uncommitted changes; every node commits its own work before the push:" >&2
  git status --short >&2
  exit 1
fi

repo=$(git remote get-url origin | sed -E 's#^.*[:/]([^/]+/[^/]+)$#\1#; s#\.git$##')
git push -u origin "$branch" >&2
pr=$(gh pr list --repo "$repo" --head "$branch" --state open --json url -q '.[0].url')
if [ -z "$pr" ]; then
  pr=$(gh pr create --repo "$repo" --base "$base" --head "$branch" --draft --title "$title" --body-file "$body")
fi
echo "{\"pr\": \"$pr\", \"sha\": \"$(git rev-parse HEAD)\"}"
