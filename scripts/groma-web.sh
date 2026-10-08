#!/usr/bin/env bash
# Opens the Groma architecture map in the browser with the weddingapp backlog attached (TASK-36), so the tickets that
# reference map elements or name changed files of this repo show on it. The tickets live in the sibling weddingapp
# checkout, beside this repo's main checkout; BACKLOG_CWD overrides that. Arguments go to `groma web` (e.g. --port).
# GROMA names the CLI (default: groma). Groma shows To Do and In Progress tickets, not Done ones.
#   scripts/groma-web.sh [--port <n>]
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ -z "${BACKLOG_CWD:-}" ]]; then
  main="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"
  BACKLOG_CWD="$(cd "$main/.." && pwd)/weddingapp"
  if [[ ! -f "$BACKLOG_CWD/backlog/config.yml" ]]; then
    echo "No weddingapp backlog at $BACKLOG_CWD; clone weddingapp beside this repo or set BACKLOG_CWD." >&2
    exit 1
  fi
fi
export BACKLOG_CWD
exec "${GROMA:-groma}" web "$@"
