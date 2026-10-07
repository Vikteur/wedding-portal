#!/usr/bin/env bash
# Reads the pinned rekord-contract ref from gradle.properties and fails unless it is exactly one
# rekordContractTag line whose value is either a vMAJOR.MINOR.PATCH tag, or the name of the branch
# being built (never on main). Writes kind=, ref=, pin= (and tag= for a tag pin) to GITHUB_OUTPUT when set.
# Build branch: GITHUB_HEAD_REF on a pull_request event, else GITHUB_REF_NAME.
# Usage: contract-pin.sh [gradle.properties]
set -euo pipefail

file="${1:-gradle.properties}"

lines="$(grep -E '^[[:space:]]*rekordContractTag[[:space:]]*[=:]' "$file" || true)"
count="$(grep -c . <<<"$lines" || true)"
if [[ "$count" -ne 1 ]]; then
  echo "FAIL: $file must name exactly one rekordContractTag, found $count" >&2
  exit 1
fi

pin="$(sed -E 's/^[[:space:]]*rekordContractTag[[:space:]]*[=:][[:space:]]*//; s/[[:space:]]*$//' <<<"$lines" | tr -d '\r')"

if [[ "${GITHUB_EVENT_NAME:-}" == "pull_request" ]]; then
  branch="${GITHUB_HEAD_REF:-}"
else
  branch="${GITHUB_REF_NAME:-}"
fi

if [[ "$pin" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  kind=tag
  ref="refs/tags/$pin"
  echo "rekord-contract tag: $pin"
else
  if [[ -z "$pin" || ! "$pin" =~ ^[A-Za-z0-9._/-]+$ || "$pin" == *..* || "$pin" == -* || "$pin" == /* ]]; then
    echo "FAIL: rekordContractTag '$pin' is neither a vMAJOR.MINOR.PATCH tag nor a valid branch name" >&2
    exit 1
  fi
  if [[ "$branch" == "main" || ( "${GITHUB_EVENT_NAME:-}" == "push" && "${GITHUB_REF_NAME:-}" == "main" ) ]]; then
    echo "FAIL: main must pin a rekord-contract tag (vMAJOR.MINOR.PATCH), found branch $pin" >&2
    exit 1
  fi
  if [[ "$pin" != "$branch" ]]; then
    echo "FAIL: a branch pin must name this branch (${branch:-<unknown>}), found $pin" >&2
    exit 1
  fi
  kind=branch
  ref="refs/heads/$pin"
  echo "rekord-contract branch: $pin (the PR cannot be green until the pin names a tag)"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "kind=$kind"
    echo "ref=$ref"
    echo "pin=$pin"
    if [[ "$kind" == "tag" ]]; then echo "tag=$pin"; fi
  } >> "$GITHUB_OUTPUT"
fi
