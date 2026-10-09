---
name: tdd-loop
description: Red-green-refactor — one failing test first, minimal code, refactor under green. Use when implementing criteria.
---
# TDD loop

## When to use
Turning acceptance criteria into code one behavior at a time. When each requirement can be pinned by a test before it exists — and you want the tests to double as the behavior spec.

## How
- **Red first.** Pick *one* behavior from the criteria; write a test that fails for the right reason. Never write production code without a failing test demanding it.
- **Watch it fail.** Run it — confirm the failure is the expected one, not a typo or wiring error.
- **Green minimal.** Write the least code that makes it pass; fake or hardcode if that's smallest. Don't build ahead of the test.
- **Refactor under green.** Clean names, dedupe, extract — with the bar green the whole time. Revert if it goes red.
- **Small steps.** One behavior per cycle; commit each green. Name tests as the behavior they assert — the suite reads as the spec.
- **Mutate before you call it done.** A green suite can still pass on the very defect it targets. Before a suite is called done:
  - Commit the implementation first. Then break the code one mutation at a time: remove each guard line once, flip a condition, shift a boundary by one. Run the suite after each. At least one test must go red; if none does, add the test that does. Revert each mutation from the commit, file by file, never with a branch-wide checkout that could wipe uncommitted work.
  - Choose mutations by the ways a defect really arrives, structural ones included: a step added after the tested one, a new file that runs the tool the gate wraps, a narrowed condition, a skip or drop rule that hides what a gate looks for.
  - Comparison helpers: one self-test per compared field, so deleting any single comparison goes red.
  - Round trips: compare against literals written before the code, never against values the code computed itself.
  - Leak or exposure tests: search for a fragment that stays whole in the output, and prove the test by turning the protection off.
  - Checks that run over the whole codebase: assert that their input is not empty, and read the real constant or list instead of a copy.

## Pattern signals (discovery cues — how the scanner recognizes this in any codebase)
- Test files paired 1:1 with units; behavior-phrased test names ("returns…/rejects…when…").
- Commit history alternating test + implementation in small increments; a fast unit-test task in CI.

## Project specifics → see docs
- Code map (test-runner setup, naming conventions, exemplar cycles) → `docs/code-maps/tdd-loop.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't write production code with no failing test asking for it — that's the whole discipline.
- Don't skip watching the test fail; a test that never went red proves nothing.
- Don't refactor while red, and don't add untested behavior "while you're there".
- Don't cram many behaviors into one cycle — split them.

## Definition of done
- [ ] Each behavior introduced via a test that failed first, then passed on minimal code.
- [ ] Refactoring done only under green; suite green at the end.
- [ ] Every guard line was removed once and a test went red; no mutation survived without a new test.
- [ ] Test names read as the behavior spec; steps small and committed per cycle.
