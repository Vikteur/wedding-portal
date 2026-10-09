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
