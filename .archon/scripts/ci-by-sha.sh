#!/usr/bin/env bash
# Wait for every CI run of the pushed HEAD commit and report the result by SHA (P5: CI is the source of truth).
#   ci-by-sha.sh [--check] [<sha>|<pr-number>]
#     print one JSON line {"state": "green|red|none|conflict", "sha": "...", "detail": "..."}; exit 0
#     --check: print the same line; exit 0 only when green (for until_bash)
#     <sha> (7-40 hex) is used as given; a plain number is a pull request, resolved to its head commit; with neither,
#     HEAD is used, but not on the default branch (main/master): a main checkout would answer for main, so exit 2.
# "none" means no run registered for the commit within the grace period: the part cannot be done without CI.
# "conflict" means the pull request cannot merge cleanly into its base: GitHub runs no pull_request CI for it, so the
# script answers at once instead of waiting. Merge the base into the branch (merge-main.sh), push, ask again.
# Failing runs are listed in detail; read them with `gh run view <id> --log-failed`.
set -euo pipefail
GRACE_POLLS=${CI_GRACE_POLLS:-20}   # x 30 s = 10 minutes for the push to register a run
MERGE_POLLS=${MERGE_POLLS:-5}       # x MERGE_WAIT s for GitHub to compute "mergeable" (it does so lazily)
MERGE_WAIT=${MERGE_WAIT:-3}

repo=$(git remote get-url origin | sed -E 's#^.*[:/]([^/]+/[^/]+)$#\1#; s#\.git$##')
check=""
pr=""   # the pull request to ask about conflicts, when there is one
if [ "${1:-}" = "--check" ]; then check=1; shift; fi
arg=${1:-}
if [[ "$arg" =~ ^[0-9a-fA-F]{7,40}$ ]]; then
  sha=$arg
elif [[ "$arg" =~ ^[0-9]+$ ]]; then
  pr=$arg
  sha=$(gh pr view "$arg" --json headRefOid -q .headRefOid)
elif [ -n "$arg" ]; then
  echo "usage: ci-by-sha.sh [--check] [<sha>|<pr-number>]" >&2; exit 2
else
  branch=$(git symbolic-ref --short -q HEAD || true)
  case "$branch" in
    main|master)
      echo "Refusing to answer for HEAD on the default branch '$branch': that is the CI of $branch, not of a pull request. Pass a <sha> or a <pr-number>, or run from the pull request worktree." >&2
      exit 2 ;;
  esac
  sha=$(git rev-parse HEAD)
  if [ "$(git rev-parse "@{upstream}" 2>/dev/null || true)" != "$sha" ]; then
    echo "HEAD $sha is not pushed; push before asking CI." >&2
  fi
  pr=$(gh pr list --repo "$repo" --head "$branch" --state open --json number -q '.[0].number' 2>/dev/null || true)
fi

# A conflicting pull request gets no CI run, so do not wait for one. UNKNOWN: GitHub has not computed it yet, ask again.
if [ -n "$pr" ]; then
  mergeable=""
  for _ in $(seq 1 "$MERGE_POLLS"); do
    mergeable=$(gh pr view "$pr" --json mergeable -q .mergeable --repo "$repo" 2>/dev/null || true)
    [ "$mergeable" != UNKNOWN ] && break
    sleep "$MERGE_WAIT"
  done
  if [ "$mergeable" = CONFLICTING ]; then
    echo "{\"state\": \"conflict\", \"sha\": \"$sha\", \"detail\": \"pull request #$pr conflicts with its base, so GitHub runs no CI; merge the base into the branch (merge-main.sh), push, ask again\"}"
    if [ -n "$check" ]; then exit 1; fi
    exit 0
  fi
fi

ids=""
for _ in $(seq 1 "$GRACE_POLLS"); do
  ids=$(gh run list --repo "$repo" --commit "$sha" --json databaseId -q '.[].databaseId')
  [ -n "$ids" ] && break
  sleep 30
done

state=green detail=""
if [ -z "$ids" ]; then
  state=none detail="no CI run registered for $sha"
else
  for id in $ids; do
    gh run watch "$id" --repo "$repo" --interval 30 --exit-status > /dev/null 2>&1 || {
      state=red
      detail="$detail $(gh run view "$id" --repo "$repo" --json name,conclusion,url -q '"\(.name)=\(.conclusion) \(.url)"')"
    }
  done
fi

echo "{\"state\": \"$state\", \"sha\": \"$sha\", \"detail\": \"${detail# }\"}"
if [ -n "$check" ] && [ "$state" != green ]; then exit 1; fi
exit 0
