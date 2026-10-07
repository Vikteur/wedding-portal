#!/usr/bin/env bash
# Tests for .archon/scripts/ci-by-sha.sh against a throwaway repo and a stubbed gh.
# Usage: bash .archon/scripts/test/ci-by-sha.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/ci-by-sha.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT
PR_SHA=abcdef0123456789abcdef0123456789abcdef01
EXPLICIT=1234567890abcdef1234567890abcdef12345678

# gh answers: `pr view <n>` gives PR_SHA, `run list` records the commit it was asked about and returns one run id.
mkdir -p "$root/bin"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
echo "gh $*" >> "$GH_LOG"
case "$1 $2" in
  "pr view") echo "$PR_SHA" ;;
  "run list") echo 11 ;;
  "run watch") exit 0 ;;
  "run view") echo "ci=failure https://example.invalid/run/11" ;;
esac
STUB
chmod +x "$root/bin/gh"

# repo <branch>: a clone of a bare origin checked out on <branch>; sets dir, head.
repo() {
  dir="$root/$1"; mkdir -p "$dir"
  git init -q --bare -b main "$dir.git"
  git clone -q "$dir.git" "$dir" 2>/dev/null
  git -C "$dir" config user.name t; git -C "$dir" config user.email t@t
  git -C "$dir" remote set-url origin https://github.com/o/repo.git
  echo base > "$dir/README.md"; git -C "$dir" add -A; git -C "$dir" commit -qm base
  [ "$2" = main ] || git -C "$dir" switch -qc "$2"
  head=$(git -C "$dir" rev-parse HEAD)
  log="$dir.log"; : > "$log"
}
run() { # run <args...>: ci-by-sha inside the repo; sets out, rc
  out=$( (cd "$dir" && PATH="$root/bin:$PATH" PR_SHA="$PR_SHA" GH_LOG="$log" CI_GRACE_POLLS=1 bash "$script" "$@") 2>&1 ); rc=$?
}
check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}

# 1. An explicit SHA is used as given, even from main.
repo one main; run "$EXPLICIT"
check "1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "1 json carries the sha" $(echo "$out" | grep -q "\"sha\": \"$EXPLICIT\""; echo $?)
check "1 ci asked about the sha" $(grep -q -- "--commit $EXPLICIT" "$log"; echo $?)

# 2. A plain number is a pull request, resolved to its head commit.
repo two main; run 5
check "2 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "2 pr resolved" $(grep -q "^gh pr view 5 --json headRefOid" "$log"; echo $?)
check "2 json carries the pr head" $(echo "$out" | grep -q "\"sha\": \"$PR_SHA\""; echo $?)
check "2 ci asked about the pr head" $(grep -q -- "--commit $PR_SHA" "$log"; echo $?)

# 3. No argument on the default branch: refused with exit 2, CI is not asked.
repo three main; run
check "3 exit 2" $([ "$rc" -eq 2 ]; echo $?)
check "3 says why" $(echo "$out" | grep -qi "default branch"; echo $?)
check "3 ci not asked" $(! grep -q "run list" "$log"; echo $?)
repo threeb master; run
check "3b exit 2 on master" $([ "$rc" -eq 2 ]; echo $?)

# 4. No argument on a feature branch: HEAD is used.
repo four feat/x; run
check "4 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "4 json carries HEAD" $(echo "$out" | grep -q "\"sha\": \"$head\""; echo $?)
check "4 ci asked about HEAD" $(grep -q -- "--commit $head" "$log"; echo $?)

# 5. --check keeps its meaning with an argument: exit 1 unless green.
repo five feat/x; run --check "$EXPLICIT"
check "5 green exits 0" $([ "$rc" -eq 0 ]; echo $?)
sed -i 's/"run watch") exit 0/"run watch") exit 1/' "$root/bin/gh"
run --check "$EXPLICIT"
check "5 red exits 1" $([ "$rc" -eq 1 ]; echo $?)
check "5 red state" $(echo "$out" | grep -q '"state": "red"'; echo $?)

echo "ci-by-sha tests: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
