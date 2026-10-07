#!/usr/bin/env bash
# Close a merged ticket out (P2: a script, not a model): commit and push its Backlog files, pull main, and delete the
# run's branch and worktree.
#   close-out.sh <TASK id or empty> <pull request url> <umbrella dir>
# Runs after `finalize` set the ticket Done. The merge was the human approval for exactly these steps (UD-21.c):
# only the ticket's and its parent's Backlog files are committed, and only the pull request's own branch is deleted.
# Every step runs; a failed one is reported and fails the node at the end, so a later step is never skipped silently.
# Prints one JSON line {"backlog": "<commit or empty>", "branch": "deleted|kept", "worktree": "removed|kept"}.
set -euo pipefail
task=$1 pr=$2 home=$3
here=$(pwd)
main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
failed=""

field() { # <task id> <field>: one field of a Backlog task, read through the CLI
  BACKLOG_CWD=$home backlog task view "$1" --json | node -e '
    let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
      const v = JSON.parse(s).task[process.argv[1]]; console.log(v == null ? "" : v);
    });' "$2"
}

number=$(gh pr view "$pr" --json number -q .number)
head=$(gh pr view "$pr" --json headRefOid -q .headRefOid)
branch=$(gh pr view "$pr" --json headRefName -q .headRefName)
repo=$(printf '%s' "$pr" | sed -E 's#^https://github.com/[^/]+/([^/]+)/pull/.*#\1#')

# 1. Commit and push the ticket's Backlog files on the umbrella's main.
backlog_commit=""
if [ -n "$task" ]; then
  status=$(field "$task" status)
  if [ "$status" != Done ]; then
    echo "finalize left $task at '$status': nothing is committed or deleted." >&2
    exit 1
  fi
  files=("$(field "$task" path)")
  parent=$(field "$task" parentTaskId)
  subject="backlog: $task done"
  if [ -n "$parent" ]; then
    files+=("$(field "$parent" path)")
    [ "$(field "$parent" status)" = Done ] && subject="backlog: $task and epic $parent done"
  fi
  if [ "$(git -C "$home" symbolic-ref --short HEAD)" != main ]; then
    failed="$failed; the umbrella checkout $home is not on main, so the Backlog change is not committed"
  elif git -C "$home" diff --quiet HEAD -- "${files[@]}"; then
    echo "No Backlog change to commit for $task." >&2
  elif git -C "$home" commit -q -m "$subject ($repo PR #$number merged, CI green on ${head:0:7})" -- "${files[@]}" \
      && git -C "$home" push -q origin main; then
    backlog_commit=$(git -C "$home" rev-parse --short HEAD)
  else
    failed="$failed; the Backlog commit or push in $home failed"
  fi
fi

# 2. Bring the main checkout up to date with the merge.
if [ "$(git -C "$main" symbolic-ref --short HEAD)" = main ]; then
  git -C "$main" pull -q --ff-only origin main || failed="$failed; pulling main in $main failed"
else
  echo "$main is not on main; not pulled." >&2
fi

# 3. Delete the pull request's own branch and the run's worktree. archon complete refuses a branch with commits that
# are not in main, so a squash-merged branch is kept and reported rather than forced.
case "$branch" in
  main|master|"") failed="$failed; refusing to delete branch '$branch'" ;;
  *)
    cd "$main"
    archon complete "$branch" >&2 || failed="$failed; archon complete $branch failed"
    ;;
esac
branch_state=deleted worktree_state=removed
if git -C "$main" ls-remote --exit-code --heads origin "$branch" > /dev/null 2>&1 \
    || git -C "$main" show-ref --verify --quiet "refs/heads/$branch"; then
  branch_state=kept
fi
if [ -d "$here" ]; then
  worktree_state=kept
fi

node -e 'const [backlog, branch, worktree] = process.argv.slice(1);
  console.log(JSON.stringify({ backlog, branch, worktree }));' "$backlog_commit" "$branch_state" "$worktree_state"

if [ -n "$failed" ] || [ "$branch_state" = kept ] || [ "$worktree_state" = kept ]; then
  echo "Close-out incomplete${failed:+:${failed#;}}. Finish by hand: archon complete $branch" >&2
  exit 1
fi
