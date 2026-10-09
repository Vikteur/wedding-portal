#!/usr/bin/env bash
# Test-first gate (P2): proves the branch was built tests first.
#
#   tdd-check.sh <artifacts-dir> <base-branch>
#
# Fails unless
#   - plan.md has no step left `- [ ]`;
#   - every checked step that is not done by the workflow's own nodes and not marked `(verify only)` (it adds no
#     behaviour: it only runs checks or reads/updates docs) has red/step-<n>.log (n = the step's
#     position among the plan's checkbox lines) showing a failing test run;
#   - every commit since the base that changes code comes after a commit holding only tests, written since
#     the code commit before it (a commit may change tests and code together only after such a test commit).
# Docs (*.md) need no test. Build files (*.gradle, *.gradle.kts, gradle/libs.versions.toml, gradle.properties,
# settings.gradle*, pom.xml, package.json, package-lock.json) are a fourth class: with tests and no code they
# belong to the test commit (test dependencies); alone they are a code commit; beside code they change nothing.
#
# Further rules:
#   - a red log whose only failure evidence is dependency resolution (Gradle "Could not resolve" / "Could not find
#     group:artifact", maven-resolver errors, npm ERESOLVE / 404 Not Found) is refused, naming the step; a compile
#     error in the same log still counts as red;
#   - a feat/fix/refactor commit that changes a test source file (src/test/, src/integrationTest/, *.test.sh,
#     *.test.ts, *Test.java, *Test.kt) is reported, unless its message body has a line starting `Test fix:` that says
#     why the test was wrong;
#   - a step marked `- [x] (pin only) ...` pins existing behaviour with tests that pass at once: its red log must
#     show the test failing against a mutation and hold a line starting `MUTATION:` that names what was mutated; it
#     needs no code commit after its test commit.
#
# Range mode: the gate can run again later over the commits after <sha> (fix rounds after review or CI). Only the
# commit order of those commits is checked (the plan and red-log checks still run), starting with no test commit
# credited. A feat/fix commit there needs a test commit before it within the range, except one that
#   - changes no behaviour files: only docs (*.md), build files (listed above) or CI config (.github/, .gitlab-ci.yml,
#     Jenkinsfile), or
#   - carries a `Test fix:` body line (the rule above).
# Such exempt commits do not count as test commits and do not consume one.
# Only the branch's own line (first parent) is walked, so commits merged in from the base are never checked.
set -euo pipefail

usage="usage: tdd-check.sh <artifacts-dir> <base-branch> [--since <sha>]"
art="${1:?$usage}"
base="${2:?$usage}"
since=""
case "${3:-}" in
  '') ;;
  --since) since="${4:-}"; [ -n "$since" ] || { echo "TDD: --since needs a commit. $usage" >&2; exit 1; } ;;
  *) echo "TDD: unknown argument ${3}. $usage" >&2; exit 1 ;;
esac
[ -z "${5:-}" ] || { echo "TDD: too many arguments. $usage" >&2; exit 1; }
plan="$art/plan.md"
errors=0
err() { echo "TDD: $*" >&2; errors=$((errors + 1)); }

[ -f "$plan" ] || { echo "TDD: no plan at $plan" >&2; exit 1; }

# Dependency-resolution evidence (gradle, maven-resolver, npm) and the generic build-failure summary lines.
dep_re='Could not resolve|Could not find [^ ]+:[^ ]+|ERESOLVE|404 Not Found|E404|aether|ArtifactResolution|Could not transfer artifact|Failed to read artifact descriptor'
summary_re='FAILURE: Build failed|BUILD FAILED|What went wrong'

# 1. Every step done, and every step the agents built has a red run.
n=0
while IFS= read -r line; do
  case "$line" in
    *'- [ ]'*) n=$((n + 1)); err "step $n is not done: $line" ;;
    *'- [x]'*|*'- [X]'*)
      n=$((n + 1))
      case "$line" in *'(done by the workflow'*|*'(verify only)'*) continue ;; esac
      log="$art/red/step-$n.log"
      if [ ! -s "$log" ]; then
        err "step $n has no red test run ($log)"
      elif ! grep -qE 'FAIL|not ok|AssertionError|error:' "$log"; then
        err "step $n: $log shows no failing test"
      elif [[ "$line" == *'(pin only)'* ]] && ! grep -q '^MUTATION:' "$log"; then
        err "step $n is (pin only): $log has no line starting \"MUTATION:\" naming what was mutated"
      elif grep -qE "$dep_re" "$log" &&
        ! grep -vE "$dep_re|$summary_re" "$log" | grep -qE 'FAIL|not ok|AssertionError|error:'; then
        err "step $n: $log fails only on dependency resolution, not on a test or a compile error"
      fi
      ;;
  esac
done < <(grep -E '^[[:space:]]*- \[[ xX]\]' "$plan")

# 2. Tests are committed before the code they drive.
if [ -n "$since" ]; then
  from="$(git rev-parse --verify --quiet "$since^{commit}")" || { echo "TDD: --since $since is not a commit" >&2; exit 1; }
else
  from="$(git merge-base HEAD "origin/$base" 2>/dev/null || git merge-base HEAD "$base")"
fi
red=0
while IFS= read -r c; do
  [ -n "$c" ] || continue
  tests=0; code=0; build=0; testsrc=0; ci=0
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    if [[ "$f" =~ (^|/)src/(test|integrationTest)/|(^|/)tests?/|\.test\.[A-Za-z]+$|(Test|Tests|IT)\.(java|kt)$ ]]; then
      tests=1
      [[ "$f" =~ (^|/)src/(test|integrationTest)/|\.test\.(sh|ts)$|Test\.(java|kt)$ ]] && testsrc=1
    elif [[ "$f" == *.md ]]; then
      :
    elif [[ "$f" =~ (^|/)(build\.gradle(\.kts)?|settings\.gradle(\.kts)?|[^/]+\.gradle(\.kts)?|libs\.versions\.toml|gradle\.properties|pom\.xml|package(-lock)?\.json)$ ]]; then
      build=1
    elif [ -n "$since" ] && [[ "$f" =~ ^\.github/|^\.gitlab-ci\.yml$|^Jenkinsfile$ ]]; then
      ci=1
    else
      code=1
    fi
  done < <(git diff-tree --no-commit-id --name-only -r "$c")
  subject="$(git log -1 --format=%s "$c")"
  testfix=0
  git log -1 --format=%b "$c" | grep -q '^Test fix:' && testfix=1
  if [ "$testsrc" -eq 1 ] && [ "$testfix" -eq 0 ] && [[ "$subject" =~ ^(feat|fix|refactor)(\(.*\))?!?: ]]; then
    err "commit ${c:0:7} \"$subject\" changes a test file; put the test in a test commit, or explain in a body line starting \"Test fix:\""
  fi
  if [ -n "$since" ]; then
    # Range mode: no behaviour files, or an explained test fix, need no test commit before them.
    [ "$tests" -eq 0 ] && [ "$code" -eq 0 ] && continue
    [ "$testfix" -eq 1 ] && continue
  fi
  [ "$build" -eq 1 ] && [ "$tests" -eq 0 ] && code=1
  if [ "$code" -eq 0 ]; then
    [ "$tests" -eq 1 ] && red=1
  elif [ "$red" -eq 1 ]; then
    red=0
  else
    err "commit ${c:0:7} \"$subject\" changes code with no test commit before it"
  fi
done < <(git rev-list --reverse --no-merges --first-parent "$from..HEAD")

if [ "$errors" -gt 0 ]; then
  echo "TDD: $errors problem(s): write the failing test first, commit it alone, then the code." >&2
  exit 1
fi
echo "TDD: tests came first on every step and every code commit"
