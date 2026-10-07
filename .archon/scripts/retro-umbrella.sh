# Sourced by the retro scripts: find_umbrella sets $umbrella to the umbrella repo (the one with backlog/config.yml),
# as a POSIX path, or returns 1. BACKLOG_CWD names it; otherwise it is searched beside the current repo's main
# checkout (same search as wait-merge.sh), because a retro is committed there, never in a run worktree.
find_umbrella() {
  local main dir
  umbrella=${BACKLOG_CWD:-}
  if [ -z "$umbrella" ]; then
    main=$(git worktree list --porcelain 2>/dev/null | sed -n '1s/^worktree //p')
    for dir in "$main" "$main"/../*; do
      if [ -n "$main" ] && [ -f "$dir/backlog/config.yml" ]; then umbrella=$dir; break; fi
    done
  fi
  if [ -z "$umbrella" ]; then
    echo "No backlog/ found beside ${main:-this repo}; set BACKLOG_CWD to the umbrella repo." >&2
    return 1
  fi
  # A POSIX path: a workflow assigns this unquoted, which would eat the backslashes of C:\...
  umbrella=$(cd "$umbrella" && pwd)
}
