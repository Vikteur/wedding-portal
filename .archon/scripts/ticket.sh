#!/usr/bin/env bash
# Read the Backlog ticket a build-feature request names and report its STOP labels (P2: a script, not a model).
#   ticket.sh "<request text>"
# Writes the ticket to $ARTIFACTS_DIR/ticket.md and prints one JSON line: {"task": "TASK-4.2", "stop": "stop-db-migration"}.
# A request without the ID of a known backlog prints {"task": "", "stop": ""}. An ID that Backlog cannot find, or
# whose prefix two backlogs share, fails the node.
#
# Each repo keeps its own tickets, and the ID's prefix names the backlog (backlog-home.sh): a run reads TASK-n from
# wedding-portal's main checkout and TOOL-n from the umbrella's, whichever repo it runs in. The repos are cloned side by
# side, not inside each other: Archon resolves a path inside the umbrella to the umbrella's codebase. BACKLOG_CWD
# names the backlog instead.
set -euo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. "$here/backlog-home.sh"

find_ticket "${1:-}"
if [ -z "$ticket" ]; then
  echo '{"task": "", "stop": ""}'
  exit 0
fi
export BACKLOG_CWD=$backlog_home
task=$ticket

json=$(backlog task view "$task" --json) || { echo "Backlog has no $task." >&2; exit 1; }
backlog task view "$task" --plain > "${ARTIFACTS_DIR:-.}/ticket.md"
printf '%s' "$json" | node -e '
  let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
    const t = JSON.parse(s).task;
    const stop = (t.labels || []).filter(l => l.startsWith("stop-")).join(",");
    console.log(JSON.stringify({ task: t.id, stop }));
  });'
