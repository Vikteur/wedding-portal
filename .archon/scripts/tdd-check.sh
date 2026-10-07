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
set -euo pipefail

art="${1:?usage: tdd-check.sh <artifacts-dir> <base-branch>}"
base="${2:?usage: tdd-check.sh <artifacts-dir> <base-branch>}"
plan="$art/plan.md"
errors=0
err() { echo "TDD: $*" >&2; errors=$((errors + 1)); }

[ -f "$plan" ] || { echo "TDD: no plan at $plan" >&2; exit 1; }

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
      fi
      ;;
  esac
done < <(grep -E '^[[:space:]]*- \[[ xX]\]' "$plan")

# 2. Tests are committed before the code they drive.
from="$(git merge-base HEAD "origin/$base" 2>/dev/null || git merge-base HEAD "$base")"
red=0
while IFS= read -r c; do
  [ -n "$c" ] || continue
  tests=0; code=0; build=0
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    if [[ "$f" =~ (^|/)src/test/|(^|/)tests?/|\.test\.[A-Za-z]+$|(Test|Tests|IT)\.java$ ]]; then
      tests=1
    elif [[ "$f" == *.md ]]; then
      :
    elif [[ "$f" =~ (^|/)(build\.gradle(\.kts)?|settings\.gradle(\.kts)?|[^/]+\.gradle(\.kts)?|libs\.versions\.toml|gradle\.properties|pom\.xml|package(-lock)?\.json)$ ]]; then
      build=1
    else
      code=1
    fi
  done < <(git diff-tree --no-commit-id --name-only -r "$c")
  subject="$(git log -1 --format=%s "$c")"
  [ "$build" -eq 1 ] && [ "$tests" -eq 0 ] && code=1
  if [ "$code" -eq 0 ]; then
    [ "$tests" -eq 1 ] && red=1
  elif [ "$red" -eq 1 ]; then
    red=0
  else
    err "commit ${c:0:7} \"$subject\" changes code with no test commit before it"
  fi
done < <(git rev-list --reverse --no-merges "$from..HEAD")

if [ "$errors" -gt 0 ]; then
  echo "TDD: $errors problem(s): write the failing test first, commit it alone, then the code." >&2
  exit 1
fi
echo "TDD: tests came first on every step and every code commit"
