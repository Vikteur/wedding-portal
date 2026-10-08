#!/usr/bin/env bash
# Checks the Groma architecture map under groma/ (TASK-36): set up for the Java scanner, curated rather than a first
# scan, every element described, in sync with the source (a fresh scan changes nothing), loaded into agent sessions, and
# opened with the weddingapp backlog attached by scripts/groma-web.sh.
# Needs the groma CLI (npm install -g groma.md) and a clean groma/ in the work tree, because it rescans. GROMA names
# the CLI (default: groma). Whatever the scan leaves in groma/ is undone when the script ends, however it ends.
# Usage: groma-check.sh
set -euo pipefail

groma="${GROMA:-groma}"

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
if [[ -d groma ]] && "$groma" agent-instructions 2>/dev/null | head -1 | grep -q 'still its first scan'; then
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
    # groma/ is clean here, so putting it back to the committed state on exit only removes what the scan wrote: a scan
    # that fails or is interrupted (Ctrl-C) must not leave a half-written map for the next push to trip over.
    restore_groma() { git checkout -q -- groma 2>/dev/null && git clean -qfd -- groma 2>/dev/null || true; }
    trap restore_groma EXIT
    trap 'exit 1' INT TERM HUP
    if ! "$groma" scan >/dev/null; then
      fail "groma scan failed"
    elif [[ -n "$(git status --porcelain -- groma)" ]]; then
      fail "a fresh groma scan changed groma/: $(git status --porcelain -- groma | tr '\n' ' ')"
    fi
    restore_groma
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

echo "Check: one command opens the map with the weddingapp backlog attached"
if [[ ! -x scripts/groma-web.sh ]]; then
  fail "scripts/groma-web.sh is missing or not executable"
else
  stub_dir="$(mktemp -d)"
  printf '#!/usr/bin/env bash\necho "$BACKLOG_CWD|$*"\n' > "$stub_dir/groma"
  chmod +x "$stub_dir/groma"
  got="$(GROMA="$stub_dir/groma" BACKLOG_CWD="$stub_dir" bash scripts/groma-web.sh --port 4799 2>&1 || true)"
  if [[ "$got" != "$stub_dir|web --port 4799" ]]; then
    fail "scripts/groma-web.sh should run 'groma web <args>' with BACKLOG_CWD set; it printed: $got"
  fi
  rm -rf "$stub_dir"
fi
if ! grep -q 'scripts/groma-web.sh' AGENTS.md 2>/dev/null; then
  fail "AGENTS.md does not name scripts/groma-web.sh"
fi

if ((failures > 0)); then
  echo "groma-check: $failures failed" >&2
  exit 1
fi
echo "groma-check: all passed"
