#!/usr/bin/env bash
# Fails unless the contract pin names a tag: a feature branch builds and tests against its contract
# branch, but it cannot be merged green until the pin names the new tag.
# Usage: contract-pin-merge-check.sh <kind: tag|branch>
set -euo pipefail

kind="${1:?usage: contract-pin-merge-check.sh <tag|branch>}"
if [[ "$kind" != "tag" ]]; then
  echo "FAIL: rekordContractTag pins a $kind; it must name a rekord-contract tag (vMAJOR.MINOR.PATCH) before this can merge" >&2
  exit 1
fi
echo "OK: the contract pin names a tag"
