#!/usr/bin/env bash
# Checks that a rekord-contract checkout is exactly at its pin.
#   kind=tag:    HEAD must equal the commit the tag points to (the tag is fetched first, because a
#                checkout of a tag ref may be shallow and not hold the tag itself).
#   kind=branch: HEAD must equal the commit origin reports for refs/heads/<pin>.
# Usage: contract-ref-check.sh <contract-checkout-dir> <kind: tag|branch> <pin>
set -euo pipefail

usage="usage: contract-ref-check.sh <contract-checkout-dir> <tag|branch> <pin>"
dir="${1:?$usage}"
kind="${2:?$usage}"
pin="${3:?$usage}"

case "$kind" in
  tag)
    git -C "$dir" fetch --depth=1 origin "refs/tags/$pin:refs/tags/$pin"
    head="$(git -C "$dir" rev-parse HEAD)"
    expected="$(git -C "$dir" rev-parse "refs/tags/$pin^{commit}")"
    ;;
  branch)
    head="$(git -C "$dir" rev-parse HEAD)"
    expected="$(git -C "$dir" ls-remote origin "refs/heads/$pin" | cut -f1)"
    ;;
  *)
    echo "FAIL: unknown pin kind '$kind' (expected tag or branch)" >&2
    exit 1
    ;;
esac

if [[ -z "$expected" || "$head" != "$expected" ]]; then
  echo "FAIL: contract HEAD $head is not $kind $pin (${expected:-not found})" >&2
  exit 1
fi

echo "OK: rekord-contract is at $kind $pin ($head)"
