#!/usr/bin/env bash
# Tests for .archon/scripts/tdd-check.sh against throwaway git repos.
# Usage: bash .archon/scripts/test/tdd-check.test.sh   (exit 0 = all pass)
set -uo pipefail

check="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/tdd-check.sh"
pass=0; fail=0

# A repo with one base commit on main and a feature branch; prints its path.
repo() {
  local d; d="$(mktemp -d)"
  git -C "$d" init -q -b main
  git -C "$d" config user.name t; git -C "$d" config user.email t@t
  mkdir -p "$d/src/main/java" "$d/src/test/java" "$d.art/red"
  echo base > "$d/README.md"; git -C "$d" add -A; git -C "$d" commit -qm base
  git -C "$d" switch -qc feat
  echo "$d"
}
commit() { # commit <dir> <message> <file>...
  local d="$1" m="$2"; shift 2
  for f in "$@"; do mkdir -p "$d/$(dirname "$f")"; echo "$RANDOM" >> "$d/$f"; done
  git -C "$d" add -A; git -C "$d" commit -qm "$m"
}
expect() { # expect <ok|fail> <name> <dir>
  local want="$1" name="$2" d="$3" got
  if (cd "$d" && bash "$check" "$d.art" main >/dev/null 2>&1); then got=ok; else got=fail; fi
  if [ "$got" = "$want" ]; then pass=$((pass+1)); else fail=$((fail+1)); echo "FAIL: $name (wanted $want, got $got)"; fi
  rm -rf "$d" "$d.art"
}
writeplan() { local d="$1"; shift; printf '%s\n' "$@" > "$d.art/plan.md"; }

# 1. Test commit, red log, then the code: passes.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test(step 1): FooTest" src/test/java/FooTest.java
echo "FooTest > foo FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat(step 1): Foo" src/main/java/Foo.java
expect ok "test first then code" "$d"

# 2. Code committed before any test: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "feat: Foo" src/main/java/Foo.java
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "BUILD FAILED" > "$d.art/red/step-1.log"
expect fail "code before test" "$d"

# 3. Test and code in one commit: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "feat: Foo with test" src/main/java/Foo.java src/test/java/FooTest.java
echo "BUILD FAILED" > "$d.art/red/step-1.log"
expect fail "test and code together" "$d"

# 4. No red log for a step: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "missing red log" "$d"

# 5. A red log that shows no failure: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "BUILD SUCCESSFUL" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "red log without a failure" "$d"

# 6. Two code commits after one test commit: the second needs its own test commit.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
commit "$d" "feat: Bar" src/main/java/Bar.java
expect fail "second code commit without a test" "$d"

# 7. Docs-only commits need no test; workflow-done steps need no red log.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)" "- [x] (done by the workflow's open-pr and ci nodes) 2. Push"
commit "$d" "docs: notes" docs/notes.md
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect ok "docs and workflow steps exempt" "$d"

# 8. Test-file shapes outside src/test count as tests (node, shell).
d=$(repo); writeplan "$d" "- [x] 1. Script (x.test.mjs)"
commit "$d" "test: x" test/x.test.mjs .archon/scripts/test/y.test.sh
echo "not ok 1 - x" > "$d.art/red/step-1.log"
commit "$d" "feat: x" scripts/x.mjs
expect ok "other test file shapes" "$d"

# 9. An unchecked step left in the plan: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)" "- [ ] 2. Add Bar (BarTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "unchecked step" "$d"

echo "tdd-check tests: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
