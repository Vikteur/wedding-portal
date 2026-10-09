#!/usr/bin/env bash
# Document a run that archon ended outright (P2: a script, not a model). A cancel, an abandon or a rejected approval
# gate ends a run before its retro nodes, so this writes the run's short section into the umbrella's
# docs/retro/<TASK or run-<id8>>/lessons-learned.md: what happened (request, where it stopped, the node timeline, the
# run that adopted it) and why it stopped (the reason stop-run.sh kept, else the gate rejection, else "no reason was
# recorded"). Then retro-commit.sh checks and commits it on the umbrella's main, and archive-runs.sh archives the finished
# runs once at the end.
#   retro-stopped.sh <run id>   that cancelled run; a stopped-run section it already has is rewritten (a reason given
#                               later), a full retro section is left alone. Fails when the run is not cancelled or the
#                               section is not committed.
#   retro-stopped.sh --sweep    every cancelled build-feature run, in every project, that has no section yet. It never
#                               fails its caller: a problem is a warning on stderr, and the next sweep tries again.
# BACKLOG_CWD names the umbrella (see retro-umbrella.sh).
set -uo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
. "$here/retro-umbrella.sh"
arg=${1:-}
if [ -z "$arg" ]; then
  echo "usage: retro-stopped.sh <run id> | --sweep" >&2
  exit 2
fi
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

on_main() {
  if [ "$(git -C "$umbrella" symbolic-ref --short HEAD 2>/dev/null || true)" != main ]; then
    echo "The umbrella checkout $umbrella is not on main, so no stopped run is documented." >&2
    return 1
  fi
}

document() { # document <add|refresh> <run id>
  local mode=$1 run=$2 dir
  if ! archon workflow get "$run" --json > "$work/get.json" 2>/dev/null; then
    echo "archon workflow get $run failed." >&2
    return 1
  fi
  archon workflow logs "$run" > "$work/transcript.jsonl" 2>/dev/null || : > "$work/transcript.jsonl"
  dir=$(node "$here/retro.js" stopped "$mode" "$work/get.json" "$work/transcript.jsonl" "$work/runs.json" "$umbrella") \
    || return 1
  if [ -z "$dir" ]; then
    echo "Run ${run:0:8} already has a retro section; left alone." >&2
    return 0
  fi
  RETRO_NO_ARCHIVE=1 bash "$here/retro-commit.sh" "$umbrella" "$dir" "$run"
}

# Every finished run is archived next to its ticket, stopped or not (archive-runs.sh); only a warning when that fails.
archive_runs() { bash "$here/archive-runs.sh" "$umbrella" >&2 || echo "warning: the Archon runs were not archived." >&2; }

if [ "$arg" != --sweep ]; then
  find_umbrella && on_main || exit 1
  archon workflow runs --all --json --limit 500 > "$work/runs.json" 2>/dev/null || echo '{"runs":[]}' > "$work/runs.json"
  document refresh "$arg"; rc=$?
  archive_runs
  exit $rc
fi

find_umbrella && on_main || exit 0
if ! archon workflow runs --all --json --limit 500 > "$work/runs.json" 2>/dev/null; then
  echo "archon workflow runs failed; stopped runs are documented by the next sweep." >&2
  exit 0
fi
for run in $(node "$here/retro.js" pending "$work/runs.json" build-feature | tr -d '\r'); do
  document add "$run" || echo "Run ${run:0:8} is not documented; the next sweep tries again." >&2
done
archive_runs
exit 0
