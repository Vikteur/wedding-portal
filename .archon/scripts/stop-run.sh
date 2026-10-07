#!/usr/bin/env bash
# Stop an archon run and write down why (P2: a script, not a model). Use it instead of a bare `archon workflow
# cancel` or `reject`, which keep no reason a retro can find.
#   stop-run.sh <run id> <reason...>
# A reason is required. It is kept beside the run's artifacts (stop-reason.json), then the run is stopped: a run
# paused at an approval gate is rejected with the reason, a running or pending one is cancelled. A run that was
# already cancelled only gets its reason. Last, retro-stopped.sh documents the run in the umbrella's docs/retro.
# A finished run (completed, failed) is refused. When the documenting fails, the run is still stopped and its reason
# kept: the next build-feature sweep, or `retro-stopped.sh <run id>`, writes it down.
set -uo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
run=${1:-}
shift || true
reason=$*
if [ -z "$run" ] || [ -z "${reason// /}" ]; then
  echo "usage: stop-run.sh <run id> <reason...>   (the reason is required: it is what the retro records)" >&2
  exit 2
fi
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

status() { archon workflow get "$run" --json > "$work/get.json" 2>/dev/null &&
  node -e 'console.log(JSON.parse(require("fs").readFileSync(process.argv[1], "utf8")).status)' "$work/get.json"; }

st=$(status) || { echo "archon workflow get $run failed: no such run?" >&2; exit 1; }
case "$st" in
  paused) action=reject ;;
  running|pending) action=cancel ;;
  cancelled) action=none ;;
  *) echo "Run ${run:0:8} has already ended ($st); there is nothing to stop." >&2; exit 1 ;;
esac

node "$here/retro.js" note "$work/get.json" "$reason" "$action" >/dev/null || exit 1
case "$action" in
  reject) archon workflow reject "$run" --reason "$reason" || exit 1 ;;
  cancel) archon workflow cancel "$run" || exit 1 ;;
esac

# Archon may take a moment to settle the run as cancelled.
for _ in $(seq 1 20); do
  st=$(status) || st=""
  [ "$st" = cancelled ] && break
  sleep 1
done
if [ "$st" != cancelled ]; then
  echo "Run ${run:0:8} is still $st after the $action; its reason is kept, the next sweep documents it." >&2
  exit 1
fi

if ! bash "$here/retro-stopped.sh" "$run"; then
  echo "Run ${run:0:8} is stopped and its reason kept, but not documented yet: run" \
    "'bash .archon/scripts/retro-stopped.sh $run', or the next build-feature sweep does it." >&2
  exit 1
fi
