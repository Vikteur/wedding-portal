#!/usr/bin/env bash
# Check and commit a run's retro on the umbrella's main (P2: a script, not a model).
#   retro-commit.sh <umbrella dir> <retro folder, e.g. docs/retro/TASK-7> <run id>
# The gate: lessons-learned.md has a "## Run <id8> — <date> — <outcome>" section with every heading of the retro skill's
# template filled in, and every adr/ADR-NN-<slug>.md has its "# ADR-NN: <title>" line and every section, and is linked
# from lessons-learned.md. Then it regenerates the index in docs/retro/README.md and commits only the retro folder and
# that index; anything else changed in the umbrella is left alone. A push rejected because main moved on is rebased
# once and pushed again. A failed gate commits nothing: fix the files and resume the run.
# Prints one JSON line {"commit": "<short sha, or empty when there was nothing to commit>"}.
set -euo pipefail
home=$1 dir=$2 run=$3
id8=${run:0:8}
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
index=docs/retro/README.md

if [ "$(git -C "$home" symbolic-ref --short HEAD 2>/dev/null || true)" != main ]; then
  echo "The umbrella checkout $home is not on main, so the retro is not committed." >&2
  exit 1
fi
node "$here/retro.js" gate "$home" "$dir" "$id8"
node "$here/retro.js" index "$home"

git -C "$home" add -- "$dir" "$index"
commit=""
if git -C "$home" diff --cached --quiet -- "$dir" "$index"; then
  echo "No retro change to commit in $dir." >&2
else
  git -C "$home" commit -q -m "retro: ${dir#docs/retro/} lessons learned and ADRs (run $id8)" -- "$dir" "$index"
  if ! git -C "$home" push -q origin main 2>/dev/null; then
    if ! git -C "$home" pull -q --rebase --autostash origin main; then
      git -C "$home" rebase --abort 2>/dev/null || true
      echo "The retro commit could not be rebased onto origin/main; it is committed locally, not pushed." >&2
      exit 1
    fi
    git -C "$home" push -q origin main
  fi
  commit=$(git -C "$home" rev-parse --short HEAD)
fi
node -e 'console.log(JSON.stringify({ commit: process.argv[1] }))' "$commit"
