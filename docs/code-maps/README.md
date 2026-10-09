# Code maps

The project facts a skill needs, one leaf per skill: `docs/code-maps/<skill name>.md`, named exactly after the skill
folder under `.claude/skills/`. The skill says *how* and holds no project nouns; the leaf says *what* for this repo
(P3): the modules, packages, classes, commands and conventions the skill applies to.

- Write a leaf when a skill first needs a project fact; extend it when the fact changes. Keep it short and current:
  describe the code as it is, never its history.
- A decision and its reasoning go in `docs/memory.md`; the leaf links to it instead of repeating it.
- The `pattern-scanner` agent writes and refreshes leaves from the code; any agent may correct a stale line.

Leaves: [jvm-testing].

[jvm-testing]: jvm-testing.md
