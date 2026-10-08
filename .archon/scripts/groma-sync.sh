#!/usr/bin/env bash
# The Groma map step of build-feature (TASK-36; P2: a script, not a model). In a repo with a groma/ map, folds a fresh
# `groma scan` into it and commits that alone, then reports whether the map still needs curating: the repo's
# .github/scripts/groma-check.sh fails, e.g. on a new element with no description. A repo without groma/ needs nothing.
# The check's output goes to stderr, so stdout holds the JSON alone. GROMA names the CLI (default: groma).
#   groma-sync.sh    prints {"curate": true|false}
set -uo pipefail
groma=${GROMA:-groma}

if [ ! -f groma/scanners.json ]; then
  echo '{"curate": false}'
  exit 0
fi
if ! command -v "$groma" > /dev/null; then
  echo "groma/ holds a Groma map but the groma CLI is missing: npm install -g groma.md" >&2
  exit 1
fi
if [ -n "$(git status --porcelain -- groma)" ]; then
  echo "There are uncommitted changes under groma/; commit or discard them first." >&2
  exit 1
fi
if ! "$groma" scan >&2; then
  echo "groma scan failed." >&2
  exit 1
fi
if [ -n "$(git status --porcelain -- groma)" ]; then
  git add -A -- groma && git commit -q -m "docs(groma): fold a fresh scan into the map" || exit 1
fi
curate=false
if [ -f .github/scripts/groma-check.sh ] && ! bash .github/scripts/groma-check.sh >&2; then
  curate=true
fi
echo "{\"curate\": $curate}"
