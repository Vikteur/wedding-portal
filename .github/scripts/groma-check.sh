#!/usr/bin/env bash
# Checks the Groma architecture map under groma/ (TASK-36): set up for the Java scanner, curated rather than a first
# scan, every element described, in sync with the source (a fresh scan changes nothing), and loaded into agent sessions.
# Needs the groma CLI (npm install -g groma.md) and a clean groma/ in the work tree, because it rescans.
# Usage: groma-check.sh
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
failures=0

fail() {
  echo "FAIL: $1" >&2
  failures=$((failures + 1))
}

echo "Check: groma/ scans the Java source"
if [[ ! -f groma/scanners.json ]]; then
  fail "groma/scanners.json is missing; run groma init"
elif ! grep -q '"id": *"java"' groma/scanners.json; then
  fail "groma/scanners.json has no java scanner"
fi

echo "Check: the map is curated, not a first scan"
if [[ -d groma ]] && groma agent-instructions 2>/dev/null | head -1 | grep -q 'still its first scan'; then
  fail "groma agent-instructions reports a first scan; curate the map"
fi

echo "Check: every element has a description"
while IFS= read -r file; do
  if ! sed -n '/^---$/,/^---$/p' "$file" | grep -qE '^description: *[^ ]'; then
    fail "$file has no description"
  fi
done < <(grep -rlE '^type: (C4|Person|External)' groma 2>/dev/null || true)

echo "Check: a fresh scan changes nothing under groma/"
if [[ -d groma ]]; then
  if [[ -n "$(git status --porcelain -- groma)" ]]; then
    fail "groma/ has uncommitted changes; commit or discard them before this check"
  else
    groma scan >/dev/null
    if [[ -n "$(git status --porcelain -- groma)" ]]; then
      fail "a fresh groma scan changed groma/: $(git status --porcelain -- groma | tr '\n' ' ')"
      git checkout -q -- groma && git clean -qfd -- groma
    fi
  fi
fi

echo "Check: agent sessions load Groma's instructions and find the tickets"
if ! grep -qx '@AGENTS.md' CLAUDE.md 2>/dev/null; then
  fail "CLAUDE.md does not import AGENTS.md"
fi
if ! grep -q 'groma agent-instructions' AGENTS.md 2>/dev/null; then
  fail "AGENTS.md has no Groma block"
fi
if ! grep -q 'weddingapp' AGENTS.md 2>/dev/null; then
  fail "AGENTS.md does not say the tickets live in the weddingapp backlog"
fi

if ((failures > 0)); then
  echo "groma-check: $failures failed" >&2
  exit 1
fi
echo "groma-check: all passed"
