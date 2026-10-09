#!/usr/bin/env bash
# Tests for .archon/scripts/decision-record.sh against throwaway plan files.
# Usage: bash .archon/scripts/test/decision-record.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/decision-record.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT
today=$(date -u +%Y-%m-%d)

run() { # run <args...>: sets out (stdout+stderr), rc
  out=$(bash "$script" "$@" 2>&1); rc=$?
}
check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}
plan() { printf '%s' "$2" > "$root/$1.md"; p="$root/$1.md"; }

# 1. Fail closed: missing plan or too few args exit 2 with usage on stderr.
run "$root/nope.md" approve "x"
check "1 missing plan exits 2" $([ "$rc" -eq 2 ]; echo $?)
check "1 usage shown" $(echo "$out" | grep -qi usage; echo $?)
plan one $'# Plan\n'
run "$p" approve
check "1 two args exits 2" $([ "$rc" -eq 2 ]; echo $?)
check "1 two args writes nothing" $([ "$(cat "$p")" = "# Plan" ]; echo $?)

# 2. Appends a new section; each non-empty line becomes a bullet.
plan two $'# Plan\n\nbody\n'
run "$p" approve $'first\n\nsecond'
check "2 approve exits 0" $([ "$rc" -eq 0 ]; echo $?)
expected=$'# Plan\n\nbody\n\n## Owner decisions\nGate answer: approve (STOP gate, '"$today"$')\n- first\n- second'
check "2 section appended" $([ "$(cat "$p")" = "$expected" ]; echo $?)
plan two_b $'# Plan\n'
run "$p" approve ""
check "2 empty text placeholder" $(grep -qxF -- '- (no comment given)' "$p"; echo $?)
check "2 other content kept" $(grep -qxF '# Plan' "$p"; echo $?)

# 2a. A plan without a final newline keeps its last line intact.
plan nonl '# Plan'
run "$p" approve "x"
check "2a heading starts its own line" $(grep -qxF '## Owner decisions' "$p"; echo $?)
check "2a last line kept" $(grep -qxF '# Plan' "$p"; echo $?)
check "2a blank line before the heading" $([ "$(sed -n 2p "$p")" = "" ]; echo $?)

# 2b. Literal text: no expansion, no backslash interpretation.
plan lit $'# Plan\n'
run "$p" approve $'cost $HOME `whoami` "q" \'s\'\nback\\nslash \\t'
check "2b literal \$HOME" $(grep -qxF -- '- cost $HOME `whoami` "q" '"'s'" "$p"; echo $?)
check "2b literal backslashes" $(grep -qxF -- '- back\nslash \t' "$p"; echo $?)

# 2c. Existing section in the middle: inserted before the next heading.
plan mid $'# Plan\n\n## Owner decisions\nGate answer: approve (STOP gate, 2026-01-01)\n- old\n\n## Tasks\n- t1\n'
run "$p" approve "new"
check "2c exits 0" $([ "$rc" -eq 0 ]; echo $?)
check "2c one heading" $([ "$(grep -c '^## Owner decisions' "$p")" -eq 1 ]; echo $?)
ln_new=$(grep -nxF -- '- new' "$p" | cut -d: -f1); ln_next=$(grep -n '^## Tasks' "$p" | cut -d: -f1)
check "2c new bullet before next heading" $([ -n "$ln_new" ] && [ "$ln_new" -lt "$ln_next" ]; echo $?)
check "2c old kept" $(grep -qxF -- '- old' "$p"; echo $?)
check "2c later section kept" $(grep -qxF -- '- t1' "$p"; echo $?)
check "2c gate line added" $(grep -qxF "Gate answer: approve (STOP gate, $today)" "$p"; echo $?)

# 2d. Existing section as last section: appended at the end.
plan last $'# Plan\n\n## Owner decisions\nGate answer: approve (STOP gate, 2026-01-01)\n- old\n'
run "$p" approve "late"
check "2d appended at end" $([ "$(tail -n 1 "$p")" = "- late" ]; echo $?)
check "2d old kept" $(grep -qxF -- '- old' "$p"; echo $?)

# 3. Reject records, then exits 1 with the message.
plan rej $'# Plan\n'
run "$p" reject "no thanks"
check "3 reject exits 1" $([ "$rc" -eq 1 ]; echo $?)
check "3 reject message" $(echo "$out" | grep -qF 'Rejected at the STOP gate: the run ends before any code.'; echo $?)
check "3 reject recorded" $(grep -qxF "Gate answer: reject (STOP gate, $today)" "$p"; echo $?)
check "3 reject bullet" $(grep -qxF -- '- no thanks' "$p"; echo $?)
# Unknown decision: exit 2, nothing written.
plan unk $'# Plan\n'
run "$p" maybe "text"
check "3 unknown exits 2" $([ "$rc" -eq 2 ]; echo $?)
check "3 unknown writes nothing" $([ "$(cat "$p")" = "# Plan" ]; echo $?)

# 4. Decision: lines are kept verbatim.
plan dec $'# Plan\n'
run "$p" approve $'Decision: go ahead\nDecision: with care'
check "4 verbatim 1" $(grep -qxF -- '- Decision: go ahead' "$p"; echo $?)
check "4 verbatim 2" $(grep -qxF -- '- Decision: with care' "$p"; echo $?)

# 5. LF only, even on Windows.
plan crlf $'# Plan\n'
run "$p" approve $'windows line\r\nsecond\r\n'
check "5 CRLF text becomes LF bullets" $(grep -qxF -- '- windows line' "$p"; echo $?)
check "5 no CR" $([ -z "$(tr -cd '\r' < "$p")" ]; echo $?)
check "5 no CR in the mid-section plan" $([ -z "$(tr -cd '\r' < "$root/mid.md")" ]; echo $?)

echo "decision-record tests: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
