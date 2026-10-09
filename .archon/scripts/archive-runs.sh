#!/usr/bin/env bash
# Archive every finished Archon run next to its ticket (P2: a script, not a model).
#   archive-runs.sh <umbrella dir>
# The run folders (~/.archon/workspaces/Vikteur/<repo>/artifacts/runs/<run id>/) stay outside the repo and temporary. This
# sweeps every run of every repo (ARCHON_HOME overrides ~/.archon): the ids in logs/*.jsonl and in artifacts/runs/*. A run
# is archived once it has finished (a workflow_complete or workflow_error event in its log, else archon says completed,
# failed or cancelled); a running run waits for the next sweep, and a run already archived in the same state is skipped.
# Each run goes to docs/retro/<TASK>/runs/<id8>/: its key files, every *.log, the agent log as transcript.jsonl and a
# run.json manifest. The ticket comes from ticket.md, else from the pull request (.pr-number or "PR #n" in the request;
# gh pr view: branch, title, body), else from the request; with none, docs/retro/unlinked/runs/<id8>/. A pull-request link
# is also written to docs/tokenomics/attribution.json, so the token reports count that run for the ticket.
# Then it commits only those runs folders on the umbrella's main and pushes (a push rejected because main moved on is
# rebased once and pushed again), and refreshes the token reports (scripts/tokenomics/commit-reports.sh): a failure there
# is only a warning, its JSON line goes to stderr. Prints one JSON line {"commit": "<short sha or empty>", "archived": [...]}.
set -euo pipefail
home=$1
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
archon_home=${ARCHON_HOME:-$HOME/.archon}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

if [ "$(git -C "$home" symbolic-ref --short HEAD 2>/dev/null || true)" != main ]; then
  echo "The umbrella checkout $home is not on main, so no run is archived." >&2
  exit 1
fi

archon workflow runs --all --json --limit 500 > "$work/runs.json" 2>/dev/null || echo '{"runs":[]}' > "$work/runs.json"
node "$here/retro.js" sweep "$home" "$archon_home" "$work/runs.json" > "$work/result.json"
mapfile -t paths < <(node -e 'for (const p of JSON.parse(require("fs").readFileSync(process.argv[1], "utf8")).paths) console.log(p)' "$work/result.json" | tr -d '\r')

commit=""
if [ "${#paths[@]}" -gt 0 ]; then
  git -C "$home" add -A -- "${paths[@]}"
  if git -C "$home" diff --cached --quiet -- "${paths[@]}"; then
    echo "No run change to commit." >&2
  else
    git -C "$home" commit -q -m "retro: archive ${#paths[@]} Archon run folder(s) of finished runs" -- "${paths[@]}"
    if ! git -C "$home" push -q origin main 2>/dev/null; then
      if ! git -C "$home" pull -q --rebase --autostash origin main; then
        git -C "$home" rebase --abort 2>/dev/null || true
        echo "The run archive commit could not be rebased onto origin/main; it is committed locally, not pushed." >&2
        exit 1
      fi
      git -C "$home" push -q origin main
    fi
    commit=$(git -C "$home" rev-parse --short HEAD)
  fi
fi
# Update and commit the token and cost reports (TASK-41): warning only, its JSON line goes to stderr.
if [ -f "$home/scripts/tokenomics/commit-reports.sh" ]; then
  "$home/scripts/tokenomics/commit-reports.sh" "$home" >&2 || echo "warning: the token reports were not committed." >&2
fi
node -e 'const r = JSON.parse(require("fs").readFileSync(process.argv[2], "utf8"));
  console.log(JSON.stringify({ commit: process.argv[1], archived: r.archived }));' "$commit" "$work/result.json"
