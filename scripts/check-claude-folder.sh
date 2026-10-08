#!/usr/bin/env bash
# Checks that the .claude folder (TASK-46) hangs together: the instruction file and settings exist, every hook script that
# settings.json names exists, and every .claude/skills/<name> path that a file under .claude/ mentions has a SKILL.md.
# Prints one line per problem and exits 1 when there is any; exits 0 and prints one line when there is none.
# Usage: bash scripts/check-claude-folder.sh
set -uo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

claude=".claude"
problems=0

problem() {
  echo "$1"
  problems=$((problems + 1))
}

if [ ! -f "$claude/CLAUDE.md" ]; then
  problem "$claude/CLAUDE.md is missing"
fi

if [ ! -f "$claude/settings.json" ]; then
  problem "$claude/settings.json is missing"
else
  for script in $(grep -oE '\.claude/hooks/scripts/[A-Za-z0-9_.-]+\.sh' "$claude/settings.json" | sort -u); do
    if [ ! -f "$script" ]; then
      problem "$claude/settings.json names $script, which does not exist"
    fi
  done
fi

if [ -d "$claude" ]; then
  # One "file:path" line per mention; the path stops at the skill name, so .../SKILL.md and placeholders like <name> fold in.
  mentions="$(grep -rEo '\.claude/skills/[A-Za-z0-9_-]+' "$claude" 2>/dev/null | sort -u)"
  while IFS=: read -r file skill_path; do
    [ -n "$file" ] || continue
    if [ ! -f "$skill_path/SKILL.md" ]; then
      problem "$file mentions $skill_path, which has no SKILL.md"
    fi
  done <<EOF_MENTIONS
$mentions
EOF_MENTIONS
fi

if [ "$problems" -gt 0 ]; then
  exit 1
fi
echo "ok - the .claude folder's hook and skill references resolve"
