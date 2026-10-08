# Sourced by the retro scripts: find_umbrella sets $umbrella to the umbrella repo, the one with the retro index
# docs/retro/README.md, as a POSIX path, or returns 1. RETRO_HOME names it; otherwise it is searched in the current
# repo's main checkout and its siblings, because a retro is committed there, never in a run worktree. Every repo has a
# backlog of its own (TASK-36), so a backlog no longer marks the umbrella, and BACKLOG_CWD names a ticket's backlog,
# not the umbrella. request_ticket and ticket_prefixes read a ticket ID of any of those backlogs.
. "$(dirname "${BASH_SOURCE[0]}")/backlog-home.sh"

find_umbrella() {
  local main dir
  umbrella=${RETRO_HOME:-}
  if [ -z "$umbrella" ]; then
    main=$(git worktree list --porcelain 2>/dev/null | sed -n '1s/^worktree //p')
    for dir in "$main" "$main"/../*; do
      if [ -n "$main" ] && [ -f "$dir/docs/retro/README.md" ]; then umbrella=$dir; break; fi
    done
  fi
  if [ -z "$umbrella" ]; then
    echo "No docs/retro/README.md found beside ${main:-this repo}; set RETRO_HOME to the umbrella repo." >&2
    return 1
  fi
  # A POSIX path: a workflow assigns this unquoted, which would eat the backslashes of C:\...
  umbrella=$(cd "$umbrella" && pwd)
}

# known_backlogs: the backlogs beside the current repo and beside the umbrella, one per line.
known_backlogs() {
  { backlog_dirs; backlog_dirs "$umbrella"
    if [ -f "$umbrella/backlog/config.yml" ]; then (cd "$umbrella" && pwd); fi; } | sort -u
}

# ticket_prefixes: their task prefixes as one alternation, e.g. task|tool.
ticket_prefixes() {
  known_backlogs | while IFS= read -r d; do if [ -n "$d" ]; then backlog_prefix "$d"; fi; done | sort -u \
    | tr '\n' '|' | sed 's/|$//'
}

# request_ticket <request>: the first ticket ID of a known backlog in the request, upper case, or nothing.
request_ticket() { ticket_id "$1" "$(known_backlogs)"; }
