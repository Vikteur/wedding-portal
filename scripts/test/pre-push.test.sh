#!/usr/bin/env bash
# Tests the versioned pre-push hook (TASK-37): .githooks/pre-push runs .github/scripts/groma-check.sh before a push and
# stops the push when the check fails or the groma CLI is missing; scripts/install-hooks.sh switches it on per clone.
# It builds throwaway git repos with a stub check script, so it tests the hook, not the real map, and does not depend on
# whether the real groma CLI is installed.
# Usage: bash scripts/test/pre-push.test.sh
set -uo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
passed=0
failed=0

check() {
  local name="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    passed=$((passed + 1))
    echo "ok   - $name"
  else
    failed=$((failed + 1))
    echo "FAIL - $name"
  fi
}

# Stubs. The check script exits with the code in $STUB_RC_FILE and leaves a marker in $STUB_RAN_FILE when it runs; both
# files live outside the work tree, so the stub never dirties it.
stub_bin="$tmp/bin"
mkdir -p "$stub_bin"
printf '#!/usr/bin/env bash\nexit 0\n' > "$stub_bin/groma"
chmod +x "$stub_bin/groma"
export STUB_RC_FILE="$tmp/rc"
export STUB_RAN_FILE="$tmp/ran"
stub_check='#!/usr/bin/env bash
echo "stub groma-check ran" >&2
echo ran > "$STUB_RAN_FILE"
exit "$(cat "$STUB_RC_FILE")"
'

# new_repo <name>: a work clone with the real hook files and the stub check, plus a bare remote with main pushed.
# The hook is not switched on yet.
new_repo() {
  local dir="$tmp/$1"
  mkdir -p "$dir"
  git init -q -b main --bare "$dir/remote.git"
  git init -q -b main "$dir/work"
  git -C "$dir/work" config user.name "Test"
  git -C "$dir/work" config user.email "test@example.com"
  git -C "$dir/work" config core.autocrlf false
  mkdir -p "$dir/work/.githooks" "$dir/work/scripts" "$dir/work/.github/scripts"
  cp "$repo_root/.githooks/pre-push" "$dir/work/.githooks/pre-push"
  cp "$repo_root/scripts/install-hooks.sh" "$dir/work/scripts/install-hooks.sh"
  printf '%s' "$stub_check" > "$dir/work/.github/scripts/groma-check.sh"
  chmod +x "$dir/work/.githooks/pre-push" "$dir/work/scripts/install-hooks.sh"
  git -C "$dir/work" add -A
  git -C "$dir/work" commit -q -m "initial"
  git -C "$dir/work" remote add origin "$dir/remote.git"
  git -C "$dir/work" push -q origin main
  echo "$dir"
}

# push_from <work-dir> <branch> <check-exit-code> <groma-cli> <stderr-file>: commits, then pushes; returns git's status.
push_from() {
  local work="$1" branch="$2" rc="$3" cli="$4" err="$5"
  echo "$rc" > "$STUB_RC_FILE"
  rm -f "$STUB_RAN_FILE"
  echo "$RANDOM$RANDOM" > "$work/change.txt"
  git -C "$work" add change.txt
  git -C "$work" commit -q -m "change"
  (cd "$work" && GROMA="$cli" git push origin "HEAD:refs/heads/$branch" 2> "$err")
}

remote_ref() { git -C "$1/remote.git" rev-parse --verify -q "refs/heads/$2" || echo none; }

# --- (a) install-hooks.sh switches the hook on, from any cwd, and is idempotent
r="$(new_repo install)"
mkdir -p "$r/work/sub/deeper"
before="$(git -C "$r/work" config --get core.hooksPath || echo unset)"
check "hooks are off before the install script runs" test "$before" = "unset"
(cd "$r/work/sub/deeper" && bash ../../scripts/install-hooks.sh) > "$tmp/install1.out" 2>&1
check "install-hooks.sh sets core.hooksPath to .githooks from a subdirectory" \
  test "$(git -C "$r/work" config --get core.hooksPath)" = ".githooks"
git -C "$r/work" config --unset core.hooksPath
(cd / && bash "$r/work/scripts/install-hooks.sh") > "$tmp/install2.out" 2>&1
check "install-hooks.sh sets core.hooksPath when run from outside the repo" \
  test "$(git -C "$r/work" config --get core.hooksPath)" = ".githooks"
bash "$r/work/scripts/install-hooks.sh" > "$tmp/install3.out" 2>&1
check "install-hooks.sh twice succeeds and leaves one value" \
  test "$(git -C "$r/work" config --get-all core.hooksPath | tr '\n' ' ')" = ".githooks "
check "install-hooks.sh prints one line" test "$(wc -l < "$tmp/install3.out" | tr -d ' ')" = "1"

# --- (b) a passing check lets the push through
r="$(new_repo pass)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
push_from "$r/work" main 0 "$stub_bin/groma" "$tmp/pass.err"
pass_rc=$?
check "a passing check lets the push succeed" test "$pass_rc" -eq 0
check "the check ran on the passing push" test -f "$STUB_RAN_FILE"
check "the remote moved to the pushed commit" \
  test "$(remote_ref "$r" main)" = "$(git -C "$r/work" rev-parse HEAD)"

# --- (c) a failing check stops the push and says what to do
r="$(new_repo fail)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
remote_before="$(remote_ref "$r" main)"
push_from "$r/work" main 1 "$stub_bin/groma" "$tmp/fail.err"
fail_rc=$?
check "a failing check rejects the push" test "$fail_rc" -ne 0
check "the remote ref is unchanged after a rejected push" test "$(remote_ref "$r" main)" = "$remote_before"
check "the rejection names groma scan" grep -q 'groma scan' "$tmp/fail.err"
check "the rejection names committing groma/" grep -q 'commit groma/' "$tmp/fail.err"
check "the output of the check itself stays visible" grep -q 'stub groma-check ran' "$tmp/fail.err"

# --- (d) a missing groma CLI stops the push with the install hint, without running the check
r="$(new_repo missing)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
remote_before="$(remote_ref "$r" main)"
push_from "$r/work" main 0 "groma-cli-that-is-not-installed" "$tmp/missing.err"
missing_rc=$?
check "a missing CLI rejects the push" test "$missing_rc" -ne 0
check "the remote ref is unchanged when the CLI is missing" test "$(remote_ref "$r" main)" = "$remote_before"
check "the rejection gives the install hint" grep -qF 'npm install -g groma.md@0.6.6' "$tmp/missing.err"
check "the check script did not run without the CLI" test ! -e "$STUB_RAN_FILE"

# --- (e) the work tree is as it was, pass or fail
r="$(new_repo tree)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
for outcome in 0 1; do
  echo "scratch" > "$r/work/untracked.txt"
  echo "edited $outcome" >> "$r/work/scripts/install-hooks.sh"
  # push_from commits change.txt only; the untracked and edited files stay in the tree
  push_from "$r/work" "tree-$outcome" "$outcome" "$stub_bin/groma" "$tmp/tree.err"
  status_before="$(git -C "$r/work" status --porcelain)"
  # a second push, so the status is taken around a hook run and nothing else
  push_from "$r/work" "tree-$outcome-again" "$outcome" "$stub_bin/groma" "$tmp/tree.err"
  status_after="$(git -C "$r/work" status --porcelain)"
  check "git status is identical before and after a push (check exit $outcome)" \
    test "$status_before" = "$status_after"
  check "the dirty tree was not empty (check exit $outcome)" test -n "$status_after"
  git -C "$r/work" checkout -q -- scripts/install-hooks.sh
done

# --- (f) a linked worktree on another branch behaves the same
r="$(new_repo worktree)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
git -C "$r/work" worktree add -q -b feature "$r/linked" main
push_from "$r/linked" feature 0 "$stub_bin/groma" "$tmp/wt-pass.err"
wt_pass_rc=$?
check "a passing push from a linked worktree succeeds" test "$wt_pass_rc" -eq 0
check "the linked worktree's branch reached the remote" \
  test "$(remote_ref "$r" feature)" = "$(git -C "$r/linked" rev-parse HEAD)"
check "the check ran from the linked worktree" test -f "$STUB_RAN_FILE"
feature_before="$(remote_ref "$r" feature)"
push_from "$r/linked" feature 1 "$stub_bin/groma" "$tmp/wt-fail.err"
wt_fail_rc=$?
check "a failing push from a linked worktree is rejected" test "$wt_fail_rc" -ne 0
check "the remote branch is unchanged after the rejected worktree push" \
  test "$(remote_ref "$r" feature)" = "$feature_before"
check "the worktree rejection names groma scan" grep -q 'groma scan' "$tmp/wt-fail.err"

# --- (g) a push that only deletes remote branches carries no code, so it is not checked; a push that also updates one is
r="$(new_repo delete)"
git -C "$r/work" push -q origin main:refs/heads/old-a main:refs/heads/old-b main:refs/heads/old-c main:refs/heads/old-d
bash "$r/work/scripts/install-hooks.sh" >/dev/null
echo 1 > "$STUB_RC_FILE"
rm -f "$STUB_RAN_FILE"
(cd "$r/work" && GROMA="$stub_bin/groma" git push origin --delete old-a old-b 2> "$tmp/delete.err")
delete_rc=$?
check "a delete-only push succeeds although the check would fail" test "$delete_rc" -eq 0
check "the check did not run on a delete-only push" test ! -e "$STUB_RAN_FILE"
check "the deleted branches are gone from the remote" \
  test "$(remote_ref "$r" old-a) $(remote_ref "$r" old-b)" = "none none"
(cd "$r/work" && GROMA="groma-cli-that-is-not-installed" git push origin --delete old-c 2> "$tmp/delete-nocli.err")
check "a delete-only push needs no groma CLI" test $? -eq 0
main_before="$(remote_ref "$r" main)"
echo "$RANDOM$RANDOM" > "$r/work/change.txt"
git -C "$r/work" add change.txt
git -C "$r/work" commit -q -m "change"
rm -f "$STUB_RAN_FILE"
(cd "$r/work" && GROMA="$stub_bin/groma" git push origin :refs/heads/old-d HEAD:refs/heads/main 2> "$tmp/mixed.err")
mixed_rc=$?
check "a push that deletes one branch and updates another is checked and rejected" test "$mixed_rc" -ne 0
check "the check ran on the mixed push" test -f "$STUB_RAN_FILE"
check "neither ref changed after the rejected mixed push" \
  test "$(remote_ref "$r" main) $(remote_ref "$r" old-d)" = "$main_before $(git -C "$r/work" rev-parse main~1)"

# --- (h) a rejection names uncommitted changes outside groma/, which the check scans along with the commits
check "a rejection from a clean tree does not mention uncommitted changes" \
  bash -c '! grep -q "uncommitted changes" "$1"' _ "$tmp/fail.err"
r="$(new_repo dirty)"
bash "$r/work/scripts/install-hooks.sh" >/dev/null
mkdir -p "$r/work/groma"
echo "half-curated" > "$r/work/groma/element.md"
push_from "$r/work" main 1 "$stub_bin/groma" "$tmp/dirty-groma.err"
check "a rejection with only groma/ uncommitted does not mention other uncommitted changes" \
  bash -c '! grep -q "uncommitted changes: commit or stash" "$1"' _ "$tmp/dirty-groma.err"
echo "scratch" > "$r/work/Scratch.java"
push_from "$r/work" main 1 "$stub_bin/groma" "$tmp/dirty-src.err"
check "a rejection with uncommitted source says to commit or stash it first" \
  grep -q 'uncommitted changes: commit or stash them first' "$tmp/dirty-src.err"

echo "pre-push tests: $passed passed, $failed failed"
[[ "$failed" -eq 0 ]]
