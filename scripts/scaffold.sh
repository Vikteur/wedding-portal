#!/usr/bin/env bash
# Render a scaffold template into a new source file (base case only; edge cases are hand-written after).
# Usage: scripts/scaffold.sh <layer/name | path.mustache> <'{json}' | vars.json> <target file>
#   e.g. scripts/scaffold.sh domain/mother '{"package": "...", "Name": "Guest", "fixture": "standardGuest",
#        "fields": [...]}' guest-domain/src/test/java/.../GuestMother.java
#   Pass JSON inline on one line: the scope guard splits multi-line commands (no heredocs).
set -euo pipefail

MUSTACHE="mustache@4.2.0"
SCAFFOLD_DIR="$(cd "$(dirname "$0")/.." && pwd)/docs/code-maps/scaffold"

if [[ $# -ne 3 ]]; then
  sed -n '3,6p' "$0" | sed 's/^# \{0,1\}//' >&2
  exit 2
fi

template="$1" vars="$2" target="$3"
[[ "$template" == *.mustache ]] || template="$SCAFFOLD_DIR/$template.mustache"
[[ -f "$template" ]] || { echo "[scaffold] template not found: $template" >&2; exit 1; }
# Never clobber code: a template only produces the first version of a file.
[[ ! -e "$target" ]] || { echo "[scaffold] target exists, refusing to overwrite: $target" >&2; exit 1; }

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
if [[ "$vars" == "{"* ]]; then
  printf '%s' "$vars" > "$tmp"
  vars="$tmp"
fi
[[ -f "$vars" ]] || { echo "[scaffold] vars file not found: $vars" >&2; exit 1; }

out="$(npx -y "$MUSTACHE" "$vars" "$template")"

mkdir -p "$(dirname "$target")"
printf '%s\n' "$out" > "$target"
echo "[scaffold] $target"
