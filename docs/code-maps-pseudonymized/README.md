---
runtime: design
---
<!-- AI_DISCLAIMER v1.0 -->
# Code maps — the project completion of generic skills (`shop-backend`, backend)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> **Generated, not hand-written (mostly).** Each `docs/code-maps/<skill>.md` is the *project data*
> that completes a generic skill: where the pattern lives in **this** repo (exemplars), the local
> conventions, frequency/coverage, and provenance. Written/refreshed by the `pattern-scanner` agent
> from `templates/code-map.template.md` (hub repo `shop-ai-hub`). A developer loads a generic
> skill **plus** its code-map. Maps and exemplar leaves are lazy-loaded project data — stated once
> here, not repeated as a banner in every file.

## Where maps live
This directory holds the **backend** maps for `shop-backend`: `<skill>.md` indexes +
`exemplars/<artifact>.md` leaves, written by the skill-harvest scan (hub repo
`workflows/skill-harvest.md`). Patterns below the harvest threshold, scan corrections, and
unmatched candidates are tracked in [`_candidates.md`](_candidates.md).

## Project config (filled by onboarding — `shop-backend`, scope=backend)
- **Languages in scope:** Java 21 production sources (`**/src/main/java/**/*.java`); Java test
  sources (`**/src/test/java/**/*.java`, scanned as a **separate pass** from production sources);
  Gradle Kotlin DSL (`**/*.gradle.kts`, `build-logic/src/main/kotlin/*.gradle.kts` for convention-plugin
  patterns). Scoped to the repo root's 14 capability triads + `common` + non-triad modules (`application`,
  `captcha-gateway`, the standalone gateway modules); excludes `**/build/**` and `**/.gradle/**`.
  — source: [_inventory] (Stack)
- **Frequency threshold:** ≥3 files across ≥2 modules (same wording as [_candidates]) — below
  this ⇒ a candidate, not a code-map. The real map count is decided by the scan itself, not assumed
  in advance.
- **Scan cadence:** on onboarding + post-merge hook.
- **Exemplar ranking:** newest/cleanest first.

## How it's produced
See `workflows/skill-harvest.md` (hub repo): scan → write code-maps + candidates → author new skills →
re-scan to fill their maps. Frequency counting runs as a script/hook (outside the context window); the
model only adjudicates ambiguous matches and authors prose.

[_inventory]: ../_inventory.md
[_candidates]: _candidates.md
