#!/usr/bin/env bash
# Tests for .archon/scripts/pr-refresh.sh against a throwaway repo and a stubbed gh.
# Usage: bash .archon/scripts/test/pr-refresh.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/pr-refresh.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

mkdir -p "$root/bin"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
echo "gh $*" >> "$GH_LOG"
STUB
chmod +x "$root/bin/gh"

check() { # check <name> <exit code of the condition>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "${out:-}" | sed 's/^/    /'; fi
}

# Origin with main, a clone on feat with two commits touching two files.
d="$root/r"; mkdir -p "$d"
git init -q --bare -b main "$d/origin.git"
git clone -q "$d/origin.git" "$d/w" 2>/dev/null
cd "$d/w" || exit 2
git config user.name t; git config user.email t@t
echo base > README.md; git add -A; git commit -qm base; git push -q origin main
git checkout -q -b feat
echo a > a.txt; git add -A; git commit -qm "feat: add a"
echo b > b.txt; git add -A; git commit -qm "fix: add b"
git fetch -q origin

art="$root/art"; mkdir -p "$art"
log="$root/gh.log"; : > "$log"
run() { out=$(PATH="$root/bin:$PATH" GH_LOG="$log" ARTIFACTS_DIR="$art" bash "$script" "$@" 2>&1); rc=$?; }

# 1. Body without a block: block appended before the final Generated-with line.
printf 'Summary\n\nWrong claim: 99 tests.\n\n🤖 Generated with [Claude Code](https://claude.com/claude-code)\n' > "$root/body.md"
run 7 main "$root/body.md"
check "1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "1 start marker" $(grep -qx '<!-- pr-refresh:start -->' "$root/body.md"; echo $?)
check "1 end marker" $(grep -qx '<!-- pr-refresh:end -->' "$root/body.md"; echo $?)
check "1 commit list" $(grep -qE '^- [0-9a-f]+ feat: add a$' "$root/body.md" && grep -qE '^- [0-9a-f]+ fix: add b$' "$root/body.md"; echo $?)
check "1 file count" $(grep -qE '(^|[^0-9])2 files? changed' "$root/body.md"; echo $?)
check "1 original text kept" $(grep -q 'Wrong claim: 99 tests.' "$root/body.md"; echo $?)
check "1 block before generated line" $([ "$(grep -n 'pr-refresh:end' "$root/body.md" | cut -d: -f1)" -lt "$(grep -n 'Generated with' "$root/body.md" | cut -d: -f1)" ]; echo $?)
check "1 gh pr edit called" $(grep -q "^gh pr edit 7 --body-file $root/body.md" "$log"; echo $?)
check "1 no owner decisions without plan" $(! grep -q 'Owner decisions' "$root/body.md"; echo $?)
first=$(cat "$root/body.md")

# 2. Idempotent.
run 7 main "$root/body.md"
check "2 identical on second run" $([ "$first" = "$(cat "$root/body.md")" ]; echo $?)
check "2 one block only" $([ "$(grep -c 'pr-refresh:start' "$root/body.md")" -eq 1 ]; echo $?)

# 3. A new commit and a plan with Owner decisions: block replaced, decisions copied, idempotent.
echo c > c.txt; git add -A; git commit -qm "test: add c"
printf '# Plan\n\n## Scope\nx\n\n## Owner decisions\n- D1: keep it small\n- D2: no cache\n\n## Other\ny\n' > "$art/plan.md"
run 7 main "$root/body.md"
check "3 new commit listed" $(grep -qE '^- [0-9a-f]+ test: add c$' "$root/body.md"; echo $?)
check "3 count updated" $(grep -qE '(^|[^0-9])3 files? changed' "$root/body.md"; echo $?)
check "3 decisions copied" $(grep -q 'D1: keep it small' "$root/body.md" && grep -q 'D2: no cache' "$root/body.md"; echo $?)
check "3 other plan sections not copied" $(! grep -q '^y$' "$root/body.md"; echo $?)
check "3 one block only" $([ "$(grep -c 'pr-refresh:start' "$root/body.md")" -eq 1 ]; echo $?)
second=$(cat "$root/body.md"); run 7 main "$root/body.md"
check "3 idempotent" $([ "$second" = "$(cat "$root/body.md")" ]; echo $?)
check "3 body text before block untouched" $(grep -q 'Wrong claim: 99 tests.' "$root/body.md"; echo $?)

# 4. Body without a Generated-with line: block appended at the end.
printf 'Just text\n' > "$root/b2.md"; run 7 main "$root/b2.md"
check "4 appended" $(grep -q 'pr-refresh:end' "$root/b2.md"; echo $?)

# 5. Usage error.
run 7 main
check "5 usage exit 2" $([ "$rc" -eq 2 ]; echo $?)

echo "pr-refresh: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
