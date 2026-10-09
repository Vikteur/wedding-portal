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

## 2026-10-09 — (no ticket) Scaffold templates render the base case
- The base case of a domain type, Mother, factory test, controller, mapper, JPA entity/repository, port and use case is
  rendered from `docs/code-maps/scaffold/<layer>/*.mustache` by `scripts/scaffold.sh` (pinned `mustache@4.2.0` via
  `npx`); agents hand-write only the edge cases. Why: deterministic boilerplate over model tokens (P2). Owner
  decision, 2026-10-09.
- Skills say "scaffold first" generically; the template list and variables live in each leaf's `## Scaffold` section
  (P3). The script never overwrites an existing file, and a wrong base case is fixed in the template, not the output.
- Variables are passed as one-line inline JSON, not a heredoc: `scope-guard.sh` splits commands on newlines.

## 2026-10-09 — (no ticket) Scaffold templates target Quarkus
- The adapter templates were ported from Spring to Quarkus: a REST resource implementing the generated JAX-RS
  interface, `Default<Name>Repository` over a package-private `PanacheRepository`, `@QuarkusTest` + `@InjectMock`
  controller tests and a Dev Services repository test. Why: the skills (`bean-config-di`, `persistence-repository`,
  `testcontainers`) target Quarkus; the worked-example leaves are Spring and give structure only. Owner decision.
- Use cases are `@ApplicationScoped` classes, not produced beans: CDI interceptors (`@Transactional`) do not apply to
  beans from a producer method. Factories stay framework-free and are produced by the bean config.
