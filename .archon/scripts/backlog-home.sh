# Sourced by ticket.sh, wait-merge.sh and the retro scripts: which Backlog a ticket lives in (P2: a script, not a model).
# Every repo keeps its own tickets in its own backlog/, and each backlog has its own task_prefix (TASK-36), so the
# ID names the backlog: TASK-7.2 is wedding-portal's, TOOL-3 the umbrella's. A backlog is searched in the current
# repo's main checkout and its siblings (the repos are cloned side by side), never in a run worktree.
#   backlog_dirs [dir]          the main checkouts with a backlog/config.yml, one POSIX path per line, own repo first
#   backlog_prefix <dir>        that backlog's task_prefix, lower case (Backlog's default: task)
#   ticket_id <text> <dirs>     the first ID of a prefix those backlogs use, upper case, or nothing
#   find_ticket <text>          sets $ticket and $backlog_home; no ID leaves both empty; two backlogs with the ID's
#                               prefix fail, naming both. BACKLOG_CWD names the backlog instead of the search.

backlog_dirs() {
  local main dir
  main=$(git -C "${1:-.}" worktree list --porcelain 2>/dev/null | sed -n '1s/^worktree //p' || true)
  [ -n "$main" ] || return 0
  if [ -f "$main/backlog/config.yml" ]; then (cd "$main" && pwd); fi
  for dir in "$main"/../*; do
    if [ -f "$dir/backlog/config.yml" ] && ! [ "$dir" -ef "$main" ]; then (cd "$dir" && pwd); fi
  done
  return 0
}

backlog_prefix() {
  local p
  p=$(tr -d '\r' < "$1/backlog/config.yml" | sed -nE 's/^task_prefix:[[:space:]]*["'\'']?([^"'\''[:space:]]+).*/\1/p' | head -1)
  printf '%s\n' "${p:-task}" | tr '[:upper:]' '[:lower:]'
}

ticket_id() {
  local alt
  alt=$(printf '%s\n' "$2" | while IFS= read -r d; do if [ -n "$d" ]; then backlog_prefix "$d"; fi; done | sort -u \
    | tr '\n' '|' | sed 's/|$//')
  [ -n "$alt" ] || return 0
  # An ID starts a word, so UTF-8 or safe-3 is no ticket of a utf or fe backlog.
  printf '%s\n' "$1" | grep -oiE "(^|[^[:alnum:]_-])($alt)-[0-9]+(\.[0-9]+)*" | head -1 \
    | sed -E 's/^[^[:alnum:]]//' | tr '[:lower:]' '[:upper:]' || true
}

find_ticket() {
  local dirs prefix homes
  ticket=""; backlog_home=""
  dirs=$(backlog_dirs)
  [ -n "${BACKLOG_CWD:-}" ] && dirs=$(printf '%s\n%s' "$(cd "$BACKLOG_CWD" && pwd)" "$dirs")
  ticket=$(ticket_id "$1" "$dirs")
  [ -n "$ticket" ] || return 0
  if [ -n "${BACKLOG_CWD:-}" ]; then
    backlog_home=$(cd "$BACKLOG_CWD" && pwd)
    return 0
  fi
  prefix=$(printf '%s' "${ticket%-*}" | tr '[:upper:]' '[:lower:]')
  homes=$(printf '%s\n' "$dirs" | while IFS= read -r d; do
    if [ -n "$d" ] && [ "$(backlog_prefix "$d")" = "$prefix" ]; then echo "$d"; fi; done)
  if [ "$(printf '%s\n' "$homes" | grep -c .)" -gt 1 ]; then
    echo "$ticket: more than one backlog uses the prefix ${prefix}: $(printf '%s' "$homes" | tr '\n' ',' \
      | sed 's/,/, /g'). Give each repo its own task_prefix, or set BACKLOG_CWD to the right one." >&2
    return 1
  fi
  backlog_home=$homes
}
