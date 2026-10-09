# Memory — decision record

Project-wide decisions for wedding-portal, and approaches tried and abandoned. Read it before you propose a change
that reverses one: the reasoning is not in the code.

How to add an entry:
- One `## <date> — <TASK id> <short title>` section per ticket, with one bullet per decision: what was decided, why,
  and the evidence (PR, CI run by commit SHA, owner decision).
- Append only. A decision that changes later gets a new dated bullet that names the one it replaces.
- Only what binds later tickets. A lesson about one run goes in the umbrella's `docs/retro/<TASK>/`; a project fact a
  skill needs goes in `docs/code-maps/<skill>.md`.

<!-- No decisions yet: the code was removed by TASK-47 (2026-10-09) to start over. -->

## 2026-10-09 — TASK-45 docs/memory.md merges as a union
- `docs/memory.md` has `merge=union` in `.gitattributes`: parallel tickets each append a section at the end, and a
  union merge keeps both sides without a hand resolution. Why: append conflicts on this file failed 14 of 18 runs
  before the restart (umbrella `docs/archon/retro-synthesis.md`, row 2). Owner decision, 2026-10-09.
- GitHub's mergeability check may not honour the union driver, so `.archon/scripts/merge-main.sh` merges `main` into
  the branch locally before `open-pr`, and again when `ci-by-sha.sh` answers `conflict`. It refuses to push a tree
  with conflict markers left.
- Keep each section self-contained: a union merge can interleave two sections' lines if both edit the same spot, so
  append a new section, never edit an older one (the append-only rule above).

## 2026-10-09 — union check A (throwaway, never merged)

Line from branch A.
