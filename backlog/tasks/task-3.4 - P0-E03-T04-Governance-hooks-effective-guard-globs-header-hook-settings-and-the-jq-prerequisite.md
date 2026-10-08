---
id: TASK-3.4
title: >-
  P0-E03-T04 Governance hooks effective: guard-globs header, hook settings and
  the jq prerequisite
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 16:00'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-2.3
references:
  - '.claude/hooks/guard-generated.globs:12-21'
  - '.claude/hooks/hooks.env:29-35'
  - 'docs/rewrite/architecture-conventions.md:977-990'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-3
priority: high
type: chore
ordinal: 304
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the write guards and hook settings to work as shipped so that generated code and the hook configuration stay protected from the first ticket on.

CONV-4: .claude/hooks/hooks.env stays as shipped, with ARCH_FITNESS_CMD unset because CI runs ArchUnit and LINT_FIX_CMD and LINT_CHECK_CMD unset until a formatter is decided (architecture-conventions §14.3; .claude/hooks/hooks.env:29-35); the guard-globs header still says the file ships empty (.claude/hooks/guard-generated.globs:12-13) while lines 20-21 already guard build/generated. PIN-17-0755 (R1, high): without jq both PreToolUse guards fail open. PIN-17-0639: the test-writer write fence.

- Covers: CONV-4, PIN-17-0755, PIN-17-0639

Plan item `P0-E03-T04` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given .claude/hooks/guard-generated.globs When it is read Then its header no longer says the file ships empty, and the patterns */build/generated/* and build/generated/* are unchanged.
- [x] #2 Given .claude/hooks/hooks.env When it is read Then ARCH_FITNESS_CMD, LINT_FIX_CMD and LINT_CHECK_CMD are unset as shipped, and docs/memory.md records that the lint commands wait for the formatter decision.
- [x] #3 Given any machine that runs the hooks of .claude/hooks for wedding-portal When the guard scripts run Then jq is on the PATH, and a Write to rekord-adapter/build/generated/openapi/HealthApi.java is denied by guard-generated.sh.
- [x] #4 Given the test-writer agent When it writes to a path outside */src/test/*, */test/* and docs/ Then the write is denied, and a write to .claude/hooks/hooks.env is denied as well.
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [x] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [x] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [x] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [x] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [x] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Validation (umbrella main 3cdaa32): scripts/test/governance-hooks.test.sh 18 passed 0 failed (AC #1 globs header + patterns; AC #2 hooks.env vars unset; AC #3 jq 1.8.2 on PATH, guard-generated denies HealthApi.java, both guards fail closed without jq; AC #4 test-writer denied src/main, README.md, hooks.env, allowed src/test, test/, docs/, and fence globs are not pathname-expanded against the cwd); framework-readiness.test.sh 51 passed; tdd-check.test.sh 12 passed.
AC #2 memory entry: wedding-portal docs/memory.md, PR #13 merged ddf4c60, checked by CodeMapLeavesTest.memory_records_that_the_lint_commands_wait_for_the_formatter_decision, CI run 37643907011 green on head 4cc71fe.
Deviation DoD #1/#2: AC #1, #3, #4 live in the umbrella's .claude/hooks, so their tests are an umbrella script, not a wedding-portal test; the umbrella has no CI, so the local runs above are the evidence (as for TASK-3.3).
Follow-up fix PR #38 (82196b4 test red 17/1, 201db1b fix, merged 3cdaa32): scope-guard.sh split the fence list with pathname expansion on, so */src/test/* expanded against the cwd (ignored planner/ and rekord-api/ in the main checkout; rekord-*/src/test in wedding-portal) and denied the test-writer its own tests. set -f around the loop.
DoD N/A: #3 no ArchUnit suite yet (TASK-3.1) and no Java changed; #4 no contract change, no generated file; #6, #7 no operation built; #9 no schema change. #5: no STOP item touched (hook governance, not user auth). #8: no secrets or personal data. #10: hooks README line and globs header updated in PR #37. #11: PRs #37 (6ee51b6), #13 (ddf4c60), #38 (3cdaa32) reviewed and merged.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Governance hooks now work as shipped: the guard-generated.globs header no longer claims the file ships empty; hooks.env keeps ARCH_FITNESS_CMD, LINT_FIX_CMD and LINT_CHECK_CMD unset, with the reason in wedding-portal docs/memory.md; guard-generated.sh and scope-guard.sh fail closed when jq is missing (PIN-17-0755); test-writer may also write a top-level test/, and scope-guard no longer glob-expands fence patterns against the cwd. Verified by scripts/test/governance-hooks.test.sh (18/18 on main 3cdaa32) and CodeMapLeavesTest in wedding-portal (CI green on 4cc71fe). PRs: weddingapp #37, #38; wedding-portal #13.
<!-- SECTION:FINAL_SUMMARY:END -->
