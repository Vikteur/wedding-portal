#!/usr/bin/env bash
# Records the owner's answer at a STOP approval gate in the plan file, then ends the run on a rejection.
# Called by an Archon bash node after an approval gate with approval.decisions: [approve, reject].
#   decision-record.sh <plan.md> <decision> <text>
#     <decision>  the gate's chosen id: approve | reject (anything else: exit 2, nothing written)
#     <text>      the reviewer's free-text reply; may be empty or multi-line; written literally, one "- " bullet per
#                 non-empty line ("- (no comment given)" when there is none)
# Appends under "## Owner decisions" (created at the end of the plan, or extended in place before the next "## "
# heading). Other content is never rewritten. Output is LF only.
# Exit: 0 approved, 1 rejected (recorded first), 2 usage error / missing plan / unknown decision (fail closed).
set -uo pipefail

usage() { echo "usage: decision-record.sh <plan.md> <approve|reject> <text>" >&2; exit 2; }

[ "$#" -ge 3 ] || usage
plan="$1"; decision="$2"; text="$3"
[ -f "$plan" ] || usage
case "$decision" in approve | reject) ;; *) usage ;; esac

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
block="$tmp/block"

{
  printf 'Gate answer: %s (STOP gate, %s)\n' "$decision" "$(date -u +%Y-%m-%d)"
  bullets=0
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    [ -n "$line" ] || continue
    printf -- '- %s\n' "$line"
    bullets=$((bullets + 1))
  done <<< "$text"
  [ "$bullets" -gt 0 ] || printf -- '- (no comment given)\n'
} > "$block"

if grep -q '^## Owner decisions[[:space:]]*$' "$plan"; then
  # Insert the block at the end of the existing section: before the next "## " heading, ahead of the blank
  # lines that separate them; at end of file when the section is the last one.
  awk -v blockfile="$block" '
    function emit(   l) {
      while ((getline l < blockfile) > 0) print l
      close(blockfile)
      for (i = 0; i < nheld; i++) print held[i]
      nheld = 0; done = 1
    }
    { sub(/\r$/, "") }
    insec && !done {
      if ($0 ~ /^## /) { emit(); insec = 0; print; next }
      if ($0 ~ /^[[:space:]]*$/) { held[nheld++] = $0; next }
      for (i = 0; i < nheld; i++) print held[i]
      nheld = 0
      print; next
    }
    !done && $0 ~ /^## Owner decisions[[:space:]]*$/ { insec = 1; print; next }
    { print }
    END { if (insec && !done) emit() }
  ' "$plan" > "$tmp/plan.new" || exit 2
else
  {
    cat "$plan"
    [ -z "$(tail -c1 "$plan")" ] || printf '\n'
    printf '\n## Owner decisions\n'
    cat "$block"
  } > "$tmp/plan.new"
fi

cat "$tmp/plan.new" > "$plan"

if [ "$decision" = reject ]; then
  echo "Rejected at the STOP gate: the run ends before any code." >&2
  exit 1
fi
exit 0
