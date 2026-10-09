#!/usr/bin/env bash
# Merge the latest base branch into the current branch, so a pull request does not turn CONFLICTING while it waits.
#   merge-main.sh [base]
#     base defaults to main. Fetches origin, merges origin/<base> with a merge commit (never a rebase).
#     print one JSON line {"merged": true|false, "pushed": true|false}; exit 0
#     merged false: the branch already contains origin/<base>, nothing was done.
#     pushed true: the merge changed the branch and it has an upstream, so it was pushed.
# Exit 1, with the reason on stderr, when:
#   - the merge stops on conflicts: it is aborted, the work tree is left as it was, and the conflicted files are named;
#   - the merge went through but brought conflict markers into the tree: nothing is pushed, the merge commit stays local.
# Files with a merge=union attribute (docs/memory.md) are merged without conflict by git itself; this script adds no merge logic.
set -euo pipefail

base=${1:-main}
git fetch -q origin "$base"
target="origin/$base"

if git merge-base --is-ancestor "$target" HEAD; then
  echo '{"merged": false, "pushed": false}'
  exit 0
fi

before=$(git rev-parse HEAD)
if ! git merge --no-edit -q "$target" > /dev/null; then
  files=$(git diff --name-only --diff-filter=U | tr '\n' ' ')
  git merge --abort
  echo "Merging $target conflicts in: ${files% }. Merge aborted; resolve by hand." >&2
  exit 1
fi

# Added lines of the merge only, so a marker that was already in the tree before is not blamed on it.
markers=$(git diff "$before" HEAD | grep -E '^\+(<<<<<<< |>>>>>>> |=======$)' || true)
if [ -n "$markers" ]; then
  echo "Merging $target left conflict markers in the tree (not pushed):" >&2
  echo "$markers" >&2
  exit 1
fi

pushed=false
if git rev-parse -q --verify '@{upstream}' > /dev/null; then
  git push -q
  pushed=true
fi
echo "{\"merged\": true, \"pushed\": $pushed}"
