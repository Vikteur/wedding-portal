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
expect() { # expect <ok|fail> <name> <dir> [extra tdd-check args]
  local want="$1" name="$2" d="$3" got; shift 3
  if (cd "$d" && bash "$check" "$d.art" main "$@" >/dev/null 2>&1); then got=ok; else got=fail; fi
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

# 10. A test commit that also changes a build file is a test commit.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest and test deps" src/test/java/FooTest.java mod/build.gradle.kts
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect ok "test commit with a build file" "$d"

# 11. A build-only commit with no test commit before it: fails.
d=$(repo); writeplan "$d" "- [x] (done by the workflow's open-pr and ci nodes) 1. Push"
commit "$d" "build: bump deps" build.gradle.kts
expect fail "build-only commit without a test" "$d"

# 12. A code commit that also changes a build file, after a test commit: passes.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo with dep" src/main/java/Foo.java build.gradle.kts
expect ok "code and build file together" "$d"

# 13. A (verify only) step needs no red log.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)" "- [x] (verify only) 2. Full local check before handing over"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect ok "verify-only step without a red log" "$d"

# 14. (verify only) does not excuse a code commit that has no test commit before it.
d=$(repo); writeplan "$d" "- [x] (verify only) 1. Check the build"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "verify-only step with an untested code commit" "$d"

# 15. Without the marker the same step still needs its red log.
d=$(repo); writeplan "$d" "- [x] 1. Full local check before handing over"
expect fail "unmarked check step needs a red log" "$d"

# 16. A red log that fails only on Gradle dependency resolution: fails.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
printf '%s\n' "FAILURE: Build failed with an exception." "* What went wrong:" \
  "Could not resolve all files for configuration ':testCompileClasspath'." \
  "> Could not find org.mockito:mockito-core:5.1." "BUILD FAILED in 2s" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "red log with only a Gradle resolution failure" "$d"

# 17. Same for npm (ERESOLVE) and maven-resolver.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
printf '%s\n' "npm ERR! code ERESOLVE" "npm ERR! ERESOLVE unable to resolve dependency tree" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "red log with only an npm ERESOLVE" "$d"
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
printf '%s\n' "[ERROR] Failed to execute goal on project x: Could not resolve dependencies for project x" \
  "Caused by: org.eclipse.aether.resolution.DependencyResolutionException: FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "red log with only a maven-resolver failure" "$d"
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
printf '%s\n' "npm ERR! 404 Not Found - GET https://registry.npmjs.org/nope" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect fail "red log with only an npm 404 for a package" "$d"

# 18. A compile error next to a resolution message is still red.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
printf '%s\n' "> Could not find org.x:y:1.0." "FooTest.java:3: error: cannot find symbol" "BUILD FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java
expect ok "compile error beside a resolution message" "$d"

# 19. A feat commit that also edits a test file: reported.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo" src/main/java/Foo.java src/test/java/FooTest.java
expect fail "feat commit changing a test file" "$d"

# 20. Other test source shapes and other types (fix, refactor) are reported too.
for shape in src/integrationTest/java/FooIT.java tools/a.test.sh web/a.test.ts src/FooTest.kt src/FooTest.java; do
  for type in fix refactor; do
    d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
    commit "$d" "test: FooTest" src/test/java/FooTest.java
    echo "FooTest FAILED" > "$d.art/red/step-1.log"
    commit "$d" "$type(step 1): Foo" src/main/java/Foo.java "$shape"
    expect fail "$type commit changing $shape" "$d"
  done
done

# 21. A "Test fix:" line in the body explains the test change: passes.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo

Test fix: FooTest asserted the wrong sign" src/main/java/Foo.java src/test/java/FooTest.java
expect ok "feat commit with a Test fix line" "$d"

# 22. "Test fix:" must start a line; mentioning it mid-sentence does not count.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
commit "$d" "feat: Foo

no Test fix: here, just prose" src/main/java/Foo.java src/test/java/FooTest.java
expect fail "Test fix not at line start" "$d"

# 23. A (pin only) step: red log with a MUTATION line, a test commit, no code commit: passes.
d=$(repo); writeplan "$d" "- [x] (pin only) 1. Pin Foo (FooTest)"
commit "$d" "test(step 1): pin Foo" src/test/java/FooTest.java
printf '%s\n' "MUTATION: Foo.bar returns 0 instead of 1" "FooTest > bar FAILED" > "$d.art/red/step-1.log"
expect ok "pin-only step with a mutation log" "$d"

# 24. A (pin only) step whose log has no MUTATION line: fails.
d=$(repo); writeplan "$d" "- [x] (pin only) 1. Pin Foo (FooTest)"
commit "$d" "test(step 1): pin Foo" src/test/java/FooTest.java
echo "FooTest > bar FAILED" > "$d.art/red/step-1.log"
expect fail "pin-only step without MUTATION" "$d"

# 25. A (pin only) step with no log, or a MUTATION line but no failure: fails.
d=$(repo); writeplan "$d" "- [x] (pin only) 1. Pin Foo (FooTest)"
commit "$d" "test(step 1): pin Foo" src/test/java/FooTest.java
expect fail "pin-only step without a log" "$d"
d=$(repo); writeplan "$d" "- [x] (pin only) 1. Pin Foo (FooTest)"
commit "$d" "test(step 1): pin Foo" src/test/java/FooTest.java
printf '%s\n' "MUTATION: Foo.bar returns 0" "BUILD SUCCESSFUL" > "$d.art/red/step-1.log"
expect fail "pin-only step whose mutated run passed" "$d"

# 26. A normal step does not become a pin-only step by logging MUTATION.
d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
commit "$d" "test: FooTest" src/test/java/FooTest.java
echo "FooTest FAILED" > "$d.art/red/step-1.log"
expect ok "normal step test commit alone" "$d"

# --since <sha>: the gate runs again over the commits after <sha> (fix rounds after review or CI).
since_repo() { # since_repo -> a repo whose first-round work (test, red log, code) is committed; prints its path
  local d; d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
  commit "$d" "test: FooTest" src/test/java/FooTest.java
  echo "FooTest FAILED" > "$d.art/red/step-1.log"
  commit "$d" "feat: Foo" src/main/java/Foo.java
  echo "$d"
}
head_of() { git -C "$1" rev-parse HEAD; }

# 27. Commits up to <sha> are not looked at; a fix after a test in the range passes.
d=$(repo); writeplan "$d" "- [x] (verify only) 1. Check"
commit "$d" "feat: untested" src/main/java/Old.java
s=$(head_of "$d")
commit "$d" "test: shows the bug" src/test/java/BugTest.java
commit "$d" "fix: the bug" src/main/java/Foo.java
expect ok "since: test then fix in the range" "$d" --since "$s"

# 28. A fix in the range with no test commit in the range fails, even if a test commit came before <sha>.
d=$(since_repo); s=$(head_of "$d")
commit "$d" "fix: review finding" src/main/java/Foo.java
expect fail "since: fix without a test in the range" "$d" --since "$s"

# 29. Docs only, or only build/CI config, need no test in the range.
d=$(since_repo); s=$(head_of "$d")
commit "$d" "docs: notes" docs/notes.md
commit "$d" "build: bump" build.gradle.kts gradle/libs.versions.toml
commit "$d" "ci: java 25" .github/workflows/ci.yml
expect ok "since: docs, build and CI config only" "$d" --since "$s"

# 30. The same build/CI-only commits are still code commits without --since.
d=$(since_repo)
commit "$d" "ci: java 25" .github/workflows/ci.yml
expect fail "no since: CI-only commit is code" "$d"

# 31. A fix that changes a test file needs Test fix:, also in the range.
d=$(since_repo); s=$(head_of "$d")
commit "$d" "test: more" src/test/java/BarTest.java
commit "$d" "fix: Foo" src/main/java/Foo.java src/test/java/FooTest.java
expect fail "since: fix editing a test without Test fix" "$d" --since "$s"
d=$(since_repo); s=$(head_of "$d")
commit "$d" "test: more" src/test/java/BarTest.java
commit "$d" "fix: Foo

Test fix: FooTest had the wrong expectation" src/main/java/Foo.java src/test/java/FooTest.java
expect ok "since: fix editing a test with Test fix" "$d" --since "$s"

# 32. A fix carrying Test fix: needs no earlier test commit in the range.
d=$(since_repo); s=$(head_of "$d")
commit "$d" "fix: Foo

Test fix: FooTest had the wrong expectation" src/main/java/Foo.java src/test/java/FooTest.java
expect ok "since: Test fix alone" "$d" --since "$s"

# 33. Bad arguments: unknown sha, missing sha, unknown flag.
d=$(since_repo)
expect fail "since: unknown sha" "$d" --since deadbeefdeadbeef
d=$(since_repo)
expect fail "since: no sha given" "$d" --since
d=$(since_repo)
expect fail "unknown flag" "$d" --bogus x

# 34. Commits brought in by merging the base are not the branch's own work.
merged_repo() { # a branch with test->code, then main gains unpaired commits and is merged in; prints its path
  local d; d=$(repo); writeplan "$d" "- [x] 1. Add Foo (FooTest)"
  commit "$d" "test: FooTest" src/test/java/FooTest.java
  echo "FooTest FAILED" > "$d.art/red/step-1.log"
  commit "$d" "feat: Foo" src/main/java/Foo.java
  git -C "$d" switch -q main
  commit "$d" "feat: other ticket (#12)" src/main/java/Other.java src/test/java/OtherTest.java
  commit "$d" "fix: unpaired" src/main/java/Other2.java
  git -C "$d" switch -q feat
  git -C "$d" merge -q --no-ff -m "Merge main into feat" main
  echo "$d"
}
d=$(merged_repo)
expect ok "merged base: base mode" "$d"
d=$(merged_repo)
# The branch tip before the merge is the first parent of the merge commit.
s=$(git -C "$d" rev-parse HEAD^1)
commit "$d" "test: BarTest" src/test/java/BarTest.java
commit "$d" "feat: Bar" src/main/java/Bar.java
expect ok "merged base: since mode" "$d" --since "$s"

echo "tdd-check tests:$pass passed, $fail failed"
[ "$fail" -eq 0 ]
