#!/usr/bin/env bash
# Names the release image: ghcr.io/<owner/name lowercased>, tagged with the full commit SHA and latest.
# Writes name=, sha= and latest= to $GITHUB_OUTPUT.
# Usage: image-tags.sh <owner/name> <40-character lowercase hex commit sha>
set -euo pipefail

repository="${1-}"
sha="${2-}"

if [[ ! "$repository" =~ ^[^/[:space:]]+/[^/[:space:]]+$ ]]; then
  echo "FAIL: the repository must be owner/name, got '$repository'" >&2
  exit 1
fi
if [[ ! "$sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo "FAIL: the commit sha must be exactly 40 lowercase hex characters, got '$sha'" >&2
  exit 1
fi

name="ghcr.io/${repository,,}"
{
  echo "name=$name"
  echo "sha=$name:$sha"
  echo "latest=$name:latest"
} >> "${GITHUB_OUTPUT:?GITHUB_OUTPUT is not set}"
echo "sha tag:    $name:$sha"
echo "latest tag: $name:latest"
