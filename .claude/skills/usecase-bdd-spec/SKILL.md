---
name: usecase-bdd-spec
description: Use-case behavior as Given/When/Then acceptance specs, observable-only. Use when specifying a use case.
---
# Use-case BDD spec

## When to use
Turning a requirement into acceptance criteria the test-writer implements against and the reviewer checks against: when you need a shared, unambiguous statement of *what the use case must do* before code exists. Authored by the analyst, consumed by the test-writer.

## How
- **Derive scenarios from the requirement**, not from any planned implementation — describe behavior a user or caller can observe.
- **One scenario per behavior**: split the happy path, each error path, and each edge/boundary into its own Given/When/Then.
- **Given** = preconditions/state; **When** = the single triggering action; **Then** = the observable outcome (result, rejection, or emitted effect).
- **Cover the unhappy paths explicitly**: invalid input, missing/forbidden access, not-found, conflicting state, empty/limit boundaries.
- **Keep it implementation-free** — no class names, layers, queries, or data structures; assert outcomes, not mechanics.
- **Make each Then checkable**: one concrete, unambiguous expectation per scenario so pass/fail is decidable.

## Pattern signals (discovery cues — how the scanner recognizes this in any codebase)
- Feature/spec files in Gherkin (`Given`/`When`/`Then`, `Scenario`, `Scenario Outline`) or a BDD runner config.
- Acceptance-criteria blocks in tickets/docs mapped 1:1 to tests; test names that read as behaviors.

## Project specifics → see docs
- Code map (spec location, scenario conventions, requirement→spec traceability) → `docs/code-maps/usecase-bdd-spec.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't leak implementation into a scenario — reference behavior, never classes, layers, or storage.
- Don't bundle multiple behaviors in one scenario, or skip the error/edge paths — those are the oracle's teeth.
- Don't write a Then that isn't observable or isn't decidably pass/fail.

## Definition of done
- [ ] Every behavior in the requirement — happy, error, and edge — has its own Given/When/Then scenario.
- [ ] Each Then is observable and unambiguously checkable; no implementation nouns anywhere.
- [ ] Specs trace back to the requirement and are ready to serve as the test-writer's source of truth.
