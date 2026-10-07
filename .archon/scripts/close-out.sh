#!/usr/bin/env bash
# Close a merged ticket out (P2: a script, not a model): commit and push its Backlog files, pull main, and delete the
# run's branch and worktree.
#   close-out.sh <TASK id or empty> <pull request url> <umbrella dir>
# Runs after `finalize` set the ticket Done. The merge was the human approval for exactly these steps (UD-21.c):
# only the ticket's and its parent's Backlog files are committed, and only the pull request's own branch is deleted.
# Every step runs; a failed one is reported and fails the node at the end, so a later step is never skipped silently.
# Prints one JSON line {"backlog": "<commit or empty>", "branch": "deleted|kept", "worktree": "removed|kept"}.
# When archon fails only because the emptied worktree directory is locked (Windows), the branch is deleted here and the
# empty directory is reported as a warning; empty unregistered directories left by earlier runs are removed first.
set -euo pipefail
task=$1 pr=$2 home=$3
here=$(pwd)
main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
here_git=$(git -C "$here" rev-parse --show-toplevel)   # git's spelling of the path, as `worktree list` prints it
failed=""
left_empty=0

registered() { # <dir>: is it a worktree git still lists?
  git -C "$main" worktree list --porcelain | sed -n 's/^worktree //p' | grep -qxF "$1"
}

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

# 3. Delete the pull request's own branch and the run's worktree. This node runs inside the still-running workflow, so
# plain `archon complete` always refuses ("running workflow"); `--force` is needed, but it also skips archon's other
# checks (commits not pushed, commits unique to the branch, uncommitted changes). So this script does its own guard
# first and only forces when the local branch tip is exactly the merged PR head and the run worktree is clean;
# otherwise archon is not called and the branch and worktree are kept and reported.
# First sweep the empty directories earlier runs left in the worktrees parent (rmdir only: a non-empty directory or a
# registered worktree is never touched).
for leftover in "$(dirname "$here_git")"/*/; do
  leftover=${leftover%/}
  [ "$leftover" = "$here_git" ] && continue
  registered "$leftover" || rmdir "$leftover" 2> /dev/null || true
done
case "$branch" in
  main|master|"") failed="$failed; refusing to delete branch '$branch'" ;;
  *)
    tip=$(git -C "$main" rev-parse --verify -q "refs/heads/$branch" || true)
    if [ -z "$tip" ]; then
      tip=$(git -C "$here" rev-parse HEAD)
    fi
    dirty=$(git -C "$here" status --porcelain)
    if [ "$tip" != "$head" ]; then
      failed="$failed; branch tip $tip is not the merged head $head"
    elif [ -n "$dirty" ]; then
      failed="$failed; worktree $here has uncommitted changes"
    else
      cd "$main"
      if ! archon complete --force "$branch" >&2; then
        # On Windows the files are gone and git has unregistered the worktree, but the directory stays because this
        # process still has it as its cwd; archon stops there. Finish what it would have done.
        if ! registered "$here_git" && { [ ! -e "$here" ] || { [ -d "$here" ] && [ -z "$(ls -A "$here")" ]; }; }; then
          if git -C "$main" show-ref --verify --quiet "refs/heads/$branch"; then
            git -C "$main" branch -D "$branch" >&2 || failed="$failed; deleting local branch $branch failed"
          fi
          if git -C "$main" ls-remote --exit-code --heads origin "$branch" > /dev/null 2>&1; then
            git -C "$main" push -q origin --delete "$branch" || failed="$failed; deleting origin/$branch failed"
          fi
          echo "warning: git no longer lists $here; the empty directory $here is left behind (a process still holds it): delete it by hand." >&2
          left_empty=1
        else
          failed="$failed; archon complete --force $branch failed"
        fi
      fi
    fi
    ;;
esac
branch_state=deleted worktree_state=removed
if git -C "$main" ls-remote --exit-code --heads origin "$branch" > /dev/null 2>&1 \
    || git -C "$main" show-ref --verify --quiet "refs/heads/$branch"; then
  branch_state=kept
fi
if [ -d "$here" ] && [ "$left_empty" -eq 0 ]; then
  worktree_state=kept
fi

node -e 'const [backlog, branch, worktree] = process.argv.slice(1);
  console.log(JSON.stringify({ backlog, branch, worktree }));' "$backlog_commit" "$branch_state" "$worktree_state"

if [ -n "$failed" ] || [ "$branch_state" = kept ] || [ "$worktree_state" = kept ]; then
  echo "Close-out incomplete${failed:+:${failed#;}}. Finish by hand: archon complete $branch" >&2
  exit 1
fi
