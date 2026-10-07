#!/usr/bin/env bash
# Read the Backlog ticket a build-feature request names and report its STOP labels (P2: a script, not a model).
#   ticket.sh "<request text>"
# Writes the ticket to $ARTIFACTS_DIR/ticket.md and prints one JSON line: {"task": "TASK-4.2", "stop": "stop-db-migration"}.
# A request without a TASK id prints {"task": "", "stop": ""}. A TASK id that Backlog cannot find fails the node.
#
# The Backlog lives in the umbrella repo. A run in the umbrella finds it in its own worktree; a run in a product repo
# finds it in a sibling of that repo's main checkout (the product repo is cloned beside the umbrella, not inside it:
# Archon resolves a path inside the umbrella to the umbrella's codebase). BACKLOG_CWD overrides.
set -euo pipefail

task=$(printf '%s' "${1:-}" | grep -oiE 'task-[0-9]+(\.[0-9]+)*' | head -1 | tr '[:lower:]' '[:upper:]' || true)
if [ -z "$task" ]; then
  echo '{"task": "", "stop": ""}'
  exit 0
fi

if [ -z "${BACKLOG_CWD:-}" ]; then
  main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
  for dir in "$PWD" "$main" "$main"/../*; do
    if [ -f "$dir/backlog/config.yml" ]; then BACKLOG_CWD=$(cd "$dir" && pwd); break; fi
  done
fi
if [ -z "${BACKLOG_CWD:-}" ]; then
  echo "No backlog/ found for $task; set BACKLOG_CWD to the umbrella repo." >&2
  exit 1
fi
export BACKLOG_CWD

json=$(backlog task view "$task" --json) || { echo "Backlog has no $task." >&2; exit 1; }
backlog task view "$task" --plain > "${ARTIFACTS_DIR:-.}/ticket.md"
printf '%s' "$json" | node -e '
  let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
    const t = JSON.parse(s).task;
    const stop = (t.labels || []).filter(l => l.startsWith("stop-")).join(",");
    console.log(JSON.stringify({ task: t.id, stop }));
  });'
