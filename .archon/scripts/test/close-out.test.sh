#!/usr/bin/env bash
# Tests for .archon/scripts/close-out.sh against throwaway repos and stubbed gh/archon.
# Usage: bash .archon/scripts/test/close-out.test.sh   (exit 0 = all pass)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/close-out.sh"
pass=0; fail=0
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

# Stubs first on PATH: gh answers from STUB_HEAD; archon models the real refusal inside a running workflow.
mkdir -p "$root/bin"
cat > "$root/bin/gh" <<'STUB'
#!/usr/bin/env bash
case "$*" in
  *number*) echo 7 ;;
  *headRefOid*) echo "$STUB_HEAD" ;;
  *headRefName*) echo feat ;;
esac
STUB
cat > "$root/bin/archon" <<'STUB'
#!/usr/bin/env bash
echo "archon $*" >> "$ARCHON_LOG"
[ "$1" = complete ] || exit 0
if [ "${2:-}" != --force ]; then
  echo "Blocked: $2 ✗ running workflow: build-feature (id: x) Use --force to override." >&2
  exit 1
fi
branch=$3
wt=$(git worktree list --porcelain | awk -v b="refs/heads/$branch" '/^worktree /{w=substr($0,10)} $1=="branch" && $2==b {print w}')
case "${STUB_MODE:-}" in
  failzero) # real archon on a half-removed worktree: prints a failed line, exits 0, leaves the registered empty folder
    find "$wt" -mindepth 1 -delete; echo "Complete: 0 completed, 1 failed" >&2; exit 0 ;;
  noop) echo "Complete: 0 completed, 1 failed" >&2; exit 0 ;;
esac
[ -n "$wt" ] && git worktree remove --force "$wt"
if [ -n "${STUB_LOCKED:-}" ]; then
  # Windows: the files are gone and git forgot the worktree, but a process holds the directory as its cwd.
  mkdir -p "$wt"
  [ "$STUB_LOCKED" = full ] && echo left > "$wt/left.txt"
  echo "error: failed to delete '$wt': Permission denied" >&2
  exit 1
fi
git branch -D "$branch" > /dev/null
git push -q origin --delete "$branch"
STUB
# git wrapper: with STUB_PERMDENIED set, `worktree remove` empties the folder, keeps it registered and fails the way
# Windows does when a process (a Gradle daemon) holds the directory.
real_git=$(command -v git)
cat > "$root/bin/git" <<STUB
#!/usr/bin/env bash
if [ -n "\${STUB_PERMDENIED:-}" ] && [ "\${1:-}" = worktree ] && [ "\${2:-}" = remove ]; then
  dir=\${!#}
  find "\$dir" -mindepth 1 -delete
  echo "error: failed to delete '\$dir': Permission denied" >&2
  exit 1
fi
exec "$real_git" "\$@"
STUB
chmod +x "$root/bin/gh" "$root/bin/archon" "$root/bin/git"

# scenario <name>: builds origin, main clone, worktree on feat with one pushed commit; sets main_dir wt head log.
scenario() {
  local d="$root/$1"; mkdir -p "$d"
  git init -q --bare -b main "$d/origin.git"
  git clone -q "$d/origin.git" "$d/main" 2>/dev/null
  git -C "$d/main" config user.name t; git -C "$d/main" config user.email t@t
  echo base > "$d/main/README.md"; git -C "$d/main" add -A; git -C "$d/main" commit -qm base
  git -C "$d/main" push -q origin main
  git -C "$d/main" worktree add -q -b feat "$d/wt"
  echo work > "$d/wt/work.txt"; git -C "$d/wt" add -A; git -C "$d/wt" commit -qm work
  git -C "$d/wt" push -q origin feat
  main_dir="$d/main"; wt="$d/wt"; log="$d/archon.log"; : > "$log"
  head=$(git -C "$wt" rev-parse HEAD)
}
run() { # run: close-out inside the worktree; sets out, rc
  out=$( (cd "$wt" && PATH="$root/bin:$PATH" STUB_HEAD="$head" ARCHON_LOG="$log" \
    bash "$script" "" https://github.com/o/repo/pull/7 "$main_dir") 2>&1 ); rc=$?
}
finish() { # finish: the re-runnable cleanup, from the main checkout; sets out, rc
  out=$( (cd "$main_dir" && PATH="$root/bin:$PATH" STUB_HEAD="$head" ARCHON_LOG="$log" \
    bash "$script" --finish "" https://github.com/o/repo/pull/7 "$main_dir") 2>&1 ); rc=$?
}
check() { # check <name> <condition exit code>
  if [ "$2" -eq 0 ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $1"; echo "$out" | sed 's/^/    /'; fi
}
called() { grep -q "^archon complete" "$log"; }

# 1. Clean worktree at the merged head: forced complete deletes branch and worktree.
scenario one; run
check "1 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "1 json deleted/removed" $(echo "$out" | grep -q '"branch":"deleted","worktree":"removed"'; echo $?)
check "1 archon --force" $(grep -q "^archon complete --force" "$log"; echo $?)

# 2. A local commit after the PR head: nothing is deleted, archon is not called.
scenario two
echo more > "$wt/more.txt"; git -C "$wt" add -A; git -C "$wt" commit -qm extra
run
check "2 exit 1" $([ "$rc" -eq 1 ]; echo $?)
check "2 archon not called" $(! called; echo $?)
check "2 branch kept" $(git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)

# 3. Uncommitted change in the worktree: nothing is deleted, archon is not called.
scenario three
echo dirty >> "$wt/work.txt"
run
check "3 exit 1" $([ "$rc" -eq 1 ]; echo $?)
check "3 archon not called" $(! called; echo $?)
check "3 worktree kept" $([ -d "$wt" ]; echo $?)

# 4. Windows: git unregistered the worktree, an empty directory is left, archon fails: branch gone, exit 0, warning.
scenario four
STUB_LOCKED=empty run
check "4 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "4 json deleted/removed" $(echo "$out" | grep -q '"branch":"deleted","worktree":"removed"'; echo $?)
check "4 local branch deleted" $(! git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "4 remote branch deleted" $(! git -C "$main_dir" ls-remote --exit-code --heads origin feat >/dev/null 2>&1; echo $?)
check "4 the emptied directory is removed, not left behind" $([ ! -e "$wt" ]; echo $?)

# 5. Unregistered but the directory still holds files: still fails, branch kept.
scenario five
STUB_LOCKED=full run
check "5 exit 1" $([ "$rc" -eq 1 ]; echo $?)
check "5 incomplete" $(echo "$out" | grep -q "Close-out incomplete"; echo $?)
check "5 branch kept" $(git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "5 files kept" $([ -f "$wt/left.txt" ]; echo $?)

# 6. An empty unregistered sibling of the run worktree is swept; a non-empty one and a registered one are not.
scenario six
d=$(dirname "$main_dir")
mkdir "$d/old-empty" "$d/old-full"; echo x > "$d/old-full/f.txt"
git -C "$main_dir" worktree add -q -b other "$d/other"
run
check "6 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "6 empty sibling swept" $([ ! -e "$d/old-empty" ]; echo $?)
check "6 non-empty sibling kept" $([ -f "$d/old-full/f.txt" ]; echo $?)
check "6 registered sibling kept" $([ -f "$d/other/README.md" ]; echo $?)
check "6 main checkout kept" $([ -d "$main_dir/.git" ]; echo $?)

# 7. Run from the main checkout (not a run worktree): the sweep is skipped, an empty folder beside the repos survives.
scenario seven
d=$(dirname "$main_dir"); mkdir "$d/empty-beside-main"
out=$( (cd "$main_dir" && PATH="$root/bin:$PATH" STUB_HEAD="$head" ARCHON_LOG="$log"   bash "$script" "" https://github.com/o/repo/pull/7 "$main_dir") 2>&1 ); rc=$?
check "7 empty folder beside the main checkout survives" $([ -d "$d/empty-beside-main" ]; echo $?)

# 8. archon prints a failed line, exits 0 and leaves the registered empty worktree: judged by state, not exit code.
scenario eight
STUB_MODE=failzero run
check "8 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "8 json deleted/removed" $(echo "$out" | grep -q '"branch":"deleted","worktree":"removed"'; echo $?)
check "8 worktree folder gone" $([ ! -e "$wt" ]; echo $?)
check "8 worktree unregistered" $(! git -C "$main_dir" worktree list --porcelain | grep -qF "$(basename "$wt")"; echo $?)
check "8 local branch deleted" $(! git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "8 remote branch deleted" $(! git -C "$main_dir" ls-remote --exit-code --heads origin feat >/dev/null 2>&1; echo $?)

# 9. archon exits 0 but did nothing: the worktree is still registered and holds files -> failed, re-runnable command.
scenario nine
STUB_MODE=noop run
check "9 exit 1" $([ "$rc" -eq 1 ]; echo $?)
check "9 branch kept" $(git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "9 files kept" $([ -f "$wt/work.txt" ]; echo $?)
check "9 prints the exact finishing command" $(echo "$out" | grep -qF -- "bash $script --finish '' https://github.com/o/repo/pull/7 $main_dir"; echo $?)
check "9 no finish by hand" $(! echo "$out" | grep -qi "by hand"; echo $?)

# 10. Squash merge: the branch is not an ancestor of main, so only a forced delete works (tip == merged head proved it).
scenario ten
git -C "$main_dir" merge -q --squash feat && git -C "$main_dir" commit -qm "squash feat" && git -C "$main_dir" push -q origin main
STUB_MODE=failzero run
check "10 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "10 local branch deleted" $(! git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "10 remote branch deleted" $(! git -C "$main_dir" ls-remote --exit-code --heads origin feat >/dev/null 2>&1; echo $?)

# 11. Permission denied: git worktree remove fails and leaves an empty registered folder -> rmdir + prune + branch -D.
scenario eleven
STUB_PERMDENIED=1 run
check "11 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "11 json deleted/removed" $(echo "$out" | grep -q '"branch":"deleted","worktree":"removed"'; echo $?)
check "11 folder gone" $([ ! -e "$wt" ]; echo $?)
check "11 local branch deleted" $(! git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)

# 12. --finish redoes only the cleanup, idempotently (and never when the tip is not the merged head).
scenario twelve
find "$wt" -mindepth 1 -delete   # a half-removed worktree left by an earlier run
finish
check "12 finish exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "12 folder gone" $([ ! -e "$wt" ]; echo $?)
check "12 local branch deleted" $(! git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)
check "12 archon not called" $(! called; echo $?)
finish
check "12 second finish exit 0" $([ "$rc" -eq 0 ]; echo $?)
scenario twelve-b
echo more > "$wt/more.txt"; git -C "$wt" add -A; git -C "$wt" commit -qm extra
finish
check "12b finish refuses a branch past the merged head" $([ "$rc" -eq 1 ]; echo $?)
check "12b branch kept" $(git -C "$main_dir" show-ref --verify --quiet refs/heads/feat; echo $?)

# 13. No step stops a process outside the worktree being closed; the forced complete cites the amended rule.
check "13 script stops no foreign process" $(! grep -Eqi 'gradlew|taskkill|pkill|killall|(^|[^a-z])kill[ ]|daemon.*--stop' "$script"; echo $?)
check "13 force cites the amended rule" $(grep -q 'UD-21.c as amended 2026-10-09' "$script"; echo $?)
# 14. The token report script is called with the umbrella dir when it exists, and the JSON line stays the only stdout line.
stub_reports() { # stub_reports <exit code>: a commit-reports.sh stub that records its arguments
  mkdir -p "$main_dir/scripts/tokenomics"
  printf '#!/usr/bin/env bash
echo "$*" > "%s/reports.args"
echo {\\"commit\\":\\"abc\\"}
exit %s
' "$(dirname "$main_dir")" "$1" > "$main_dir/scripts/tokenomics/commit-reports.sh"
  chmod +x "$main_dir/scripts/tokenomics/commit-reports.sh"
}
run_split() { # like run, but keeps stdout apart: sets out (stderr and stdout), json (stdout), rc
  json=$( (cd "$wt" && PATH="$root/bin:$PATH" STUB_HEAD="$head" ARCHON_LOG="$log"     bash "$script" "" https://github.com/o/repo/pull/7 "$main_dir") 2> "$root/stderr.txt" ); rc=$?
  out="$json
$(cat "$root/stderr.txt")"
}
scenario fourteen; stub_reports 0; run_split
check "14 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "14 report script called with the umbrella dir" $([ "$(cat "$(dirname "$main_dir")/reports.args")" = "$main_dir" ]; echo $?)
check "14 stdout is the unchanged JSON line" $([ "$json" = '{"backlog":"","branch":"deleted","worktree":"removed"}' ]; echo $?)

# 15. A failing report script is only a warning.
scenario fifteen; stub_reports 1; run_split
check "15 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "15 report script was called" $([ -f "$(dirname "$main_dir")/reports.args" ]; echo $?)
check "15 warning on stderr" $(grep -q "token reports were not committed" "$root/stderr.txt"; echo $?)
check "15 stdout is the unchanged JSON line" $([ "$json" = '{"backlog":"","branch":"deleted","worktree":"removed"}' ]; echo $?)

# 16. Without the report script nothing is called and nothing is said about it.
scenario sixteen; run_split
check "16 exit 0" $([ "$rc" -eq 0 ]; echo $?)
check "16 no mention of the token reports" $(! grep -q "token reports" "$root/stderr.txt"; echo $?)

echo "close-out tests: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
