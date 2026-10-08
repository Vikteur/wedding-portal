#!/usr/bin/env bash
# Switches on the versioned git hooks (.githooks/) for this clone: once per clone, from any directory inside it.
# Linked worktrees share the clone's git config, so this covers them too. Safe to run again.
# Usage: bash scripts/install-hooks.sh
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$repo_root" config core.hooksPath .githooks
echo "Git hooks switched on: core.hooksPath = .githooks (pre-push runs .github/scripts/groma-check.sh)"
