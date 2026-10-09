#!/usr/bin/env bash
# Render a scaffold template into a new source file (base case only; edge cases are hand-written after).
# Usage: scripts/scaffold.sh [--test] <layer/name | path.mustache> <'{json}' | vars.json> <target file>
#   e.g. scripts/scaffold.sh --test domain/mother '{"package": "...", "Name": "Guest", "fixture": "standardGuest",
#        "fields": [...]}' guest-domain/src/test/java/.../GuestMother.java
#   --test: the target must sit in a test source tree (the test-writer's fence). Pass JSON inline on one line:
#   the scope guard splits multi-line commands (no heredocs).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
SCAFFOLD_DIR="$root/docs/Moustache scripts"
# mustache@4.2.0, pinned with its integrity hash in scripts/scaffold/package-lock.json; npm ci installs it on first use.
TOOL_DIR="$root/scripts/scaffold"

test_only=0
if [[ "${1:-}" == "--test" ]]; then test_only=1; shift; fi
if [[ $# -ne 3 ]]; then
  sed -n '3,7p' "$0" | sed 's/^# \{0,1\}//' >&2
  exit 2
fi

template="$1" vars="$2" target="$3"
[[ "$template" == *.mustache ]] || template="$SCAFFOLD_DIR/$template.mustache"
[[ -f "$template" ]] || { echo "[scaffold] template not found: $template" >&2; exit 1; }

# Only relative targets inside the working tree.
t="${target//\\//}"
case "$t" in
  /*|[A-Za-z]:/*) echo "[scaffold] target must be a relative path: $target" >&2; exit 1 ;;
  ..|../*|*/..|*/../*) echo "[scaffold] target may not contain '..': $target" >&2; exit 1 ;;
esac
if [[ $test_only -eq 1 ]]; then
  case "$t" in
    src/test/*|*/src/test/*|test/*|*/test/*) ;;
    *) echo "[scaffold] --test: target must be in a test source tree (src/test/ or test/): $target" >&2; exit 1 ;;
  esac
fi
# Never clobber code: a template only produces the first version of a file.
[[ ! -e "$target" ]] || { echo "[scaffold] target exists, refusing to overwrite: $target" >&2; exit 1; }

renderer="$TOOL_DIR/node_modules/mustache/bin/mustache"
[[ -f "$renderer" ]] || npm ci --prefix "$TOOL_DIR" --silent --no-audit --no-fund >&2

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
if [[ "$vars" == "{"* ]]; then
  printf '%s' "$vars" > "$tmp"
  vars="$tmp"
fi
[[ -f "$vars" ]] || { echo "[scaffold] vars file not found: $vars" >&2; exit 1; }

out="$(node "$renderer" "$vars" "$template")"

mkdir -p "$(dirname "$target")"
printf '%s\n' "$out" > "$target"
echo "[scaffold] $target"
