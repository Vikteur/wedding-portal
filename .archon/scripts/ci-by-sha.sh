#!/usr/bin/env bash
# Wait for every CI run of the pushed HEAD commit and report the result by SHA (P5: CI is the source of truth).
#   ci-by-sha.sh            print one JSON line {"state": "green|red|none", "sha": "...", "detail": "..."}; exit 0
#   ci-by-sha.sh --check    print the same line; exit 0 only when green (for until_bash)
# "none" means no run registered for the commit within the grace period: the part cannot be done without CI.
# Failing runs are listed in detail; read them with `gh run view <id> --log-failed`.
set -euo pipefail
GRACE_POLLS=${CI_GRACE_POLLS:-20}   # x 30 s = 10 minutes for the push to register a run

repo=$(git remote get-url origin | sed -E 's#^.*[:/]([^/]+/[^/]+)$#\1#; s#\.git$##')
sha=$(git rev-parse HEAD)
if [ "$(git rev-parse "@{upstream}" 2>/dev/null || true)" != "$sha" ]; then
  echo "HEAD $sha is not pushed; push before asking CI." >&2
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
if [ "${1:-}" = "--check" ] && [ "$state" != green ]; then exit 1; fi
exit 0
