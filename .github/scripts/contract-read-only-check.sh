#!/usr/bin/env bash
# Checks that the credentials of a rekord-contract checkout cannot push: a dry-run push over HTTPS still asks
# GitHub for git-receive-pack, which a read-only token is refused, and a dry run never writes a ref.
# Fails when the push is accepted, and when it fails for any reason other than a refusal.
# Usage: contract-read-only-check.sh <contract-checkout-dir>
set -euo pipefail

dir="${1:?usage: contract-read-only-check.sh <contract-checkout-dir>}"

status=0
output="$(git -C "$dir" push --dry-run origin HEAD:refs/heads/ci-read-only-probe 2>&1)" || status=$?

if [[ $status -eq 0 ]]; then
  echo "FAIL: the contract token can push" >&2
  exit 1
fi

if ! grep -Eq '403|denied|Permission' <<<"$output"; then
  echo "FAIL: the push failed, but not with a refusal:" >&2
  echo "$output" >&2
  exit 1
fi

echo "OK: push to rekord-contract refused"
