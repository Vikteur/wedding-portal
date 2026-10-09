#!/usr/bin/env bash
# Tests for .archon/scripts/ticket-files.sh against stubbed gh and backlog.
# Usage: bash .archon/scripts/test/ticket-files.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/ticket-files.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

mkdir -p "$root/bin" "$root/home"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
echo "gh $*" >> "$CALL_LOG"
[ "$1 $2" = "pr view" ] && printf '%b' "$STUB_FILES"
exit 0
STUB
cat > "$root/bin/backlog" <<'STUB'
#!/usr/bin/env bash
echo "backlog cwd=${BACKLOG_CWD:-} $*" >> "$CALL_LOG"
exit "${STUB_BACKLOG_RC:-0}"
STUB
chmod +x "$root/bin/gh" "$root/bin/backlog"

log="$root/calls.log"
check() { if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "${out:-}" | sed 's/^/    /'; fi; }
run() { : > "$log"; out=$(PATH="$root/bin:$PATH" CALL_LOG="$log" STUB_FILES="$STUB_FILES" bash "$script" "$@" 2>&1); rc=$?; }

# 1. Every PR file becomes one --modified-file, run in the backlog home.
STUB_FILES='src/A.java\ndocs/b file.md\n'
run TASK-9 12 "$root/home"
check "1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "1 gh asked for the PR files" $(grep -qF 'gh pr view 12 --json files --jq .files[].path' "$log"; echo $?)
check "1 backlog in backlog home" $(grep -q "backlog cwd=$root/home task edit TASK-9" "$log"; echo $?)
check "1 file A" $(grep -q -- '--modified-file src/A.java' "$log"; echo $?)
check "1 file with space kept whole" $(grep -q -- '--modified-file docs/b file.md' "$log"; echo $?)
check "1 one backlog call" $([ "$(grep -c '^backlog' "$log")" -eq 1 ]; echo $?)

# 2. Empty file list: nothing to set, no backlog call, exit 1.
STUB_FILES=''
run TASK-9 12 "$root/home"
check "2 exit 1" $([ "$rc" -eq 1 ]; echo $?)
check "2 no backlog call" $(! grep -q '^backlog' "$log"; echo $?)

# 3. Backlog failure propagates.
STUB_FILES='a.txt\n'
STUB_BACKLOG_RC=3 run TASK-9 12 "$root/home"
check "3 failure propagates" $([ "$rc" -ne 0 ]; echo $?)

# 4. Usage error.
run TASK-9 12
check "4 usage exit 2" $([ "$rc" -eq 2 ]; echo $?)

echo "ticket-files: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
