#!/usr/bin/env bash
# Set a Backlog ticket's modified files to exactly the files of its merged pull request.
#   ticket-files.sh <task> <pr> <backlog-home>
#     reads `gh pr view <pr> --json files`, then `backlog task edit <task> --modified-file <path>...` in <backlog-home>.
#     `--modified-file` SETS the list (it replaces, it does not append), so the ticket matches the PR exactly.
#     exit 2 usage; exit 1 when the PR lists no files (nothing is changed); else the backlog CLI's exit code.
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "usage: ticket-files.sh <task> <pr> <backlog-home>" >&2; exit 2
fi
task=$1; pr=$2; home=$3

list=$(gh pr view "$pr" --json files --jq '.files[].path')
if [ -z "$list" ]; then
  echo "ticket-files: PR $pr lists no files; $task left unchanged" >&2; exit 1
fi

args=()
while IFS= read -r path; do
  [ -n "$path" ] && args+=(--modified-file "$path")
done <<< "$list"

BACKLOG_CWD=$home backlog task edit "$task" "${args[@]}"
