---
id: TASK-3.3
title: >-
  P0-E03-T03 Quarkus-ready skill bodies, code-map leaves and rule lines for the
  agent framework
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 14:03'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-1.1
references:
  - 'docs/rewrite/architecture-conventions.md:1109-1150'
  - .claude/rules/domain.md
  - .claude/rules/platform.md
  - 'docs/rewrite/architecture-conventions.md:126-160'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-3
priority: medium
type: docs
ordinal: 303
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the skills, code-map leaves and rule files to speak Quarkus so that no agent follows Spring advice into the new code.

architecture-conventions §16.2 items 1 to 3 (CONV-1, CONV-2, CONV-3): nine skill bodies carry Spring counterparts, the skills point at code-map leaves that do not exist, and three rule lines carry Spring or contradicting wording. Skill bodies stay free of project nouns (constitution P3); project facts go into the leaves. Rule globs need no change because the module names of §2.2 already match them.

- Covers: CONV-1, CONV-2, CONV-3

Plan item `P0-E03-T03` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the skills spring-boot-slice-tests, testcontainers, persistence-repository, resilience4j, spring-caching, exception-to-http, bean-config-di, scheduled-tasks and domain-events under .claude/skills When their bodies are read Then each has a Quarkus counterpart or a framework-neutral body with the framework named in a code-map leaf, and no body names a wedding-portal noun.
- [x] #2 Given the leaves security-review.md, jvm-testing.md, code-generation.md, spring-caching.md, java.md, archunit-fitness.md, conventional-commits.md and scheduled-tasks.md under docs/code-maps and the record docs/memory.md When every skill link to them is followed Then each target exists and states the fact the skill points at.
- [x] #3 Given .claude/rules/domain.md lines 14 and 16 and .claude/rules/platform.md line 15 after the human edit When they are read Then line 14 names no framework as its example, line 16 says events are raised in the domain and published by the use case, and the platform line names configuration, authentication mechanisms and interceptors instead of filter chains and aspects.
- [x] #4 Given the paths: globs of the rule files under .claude/rules When one main source path each of rekord-domain, rekord-usecase, rekord-adapter, rekord-gateway, application and logging is matched against them Then each path matches the globs of exactly one rule file, the one architecture-conventions §2.2 lists for its module, and general.md, which has no globs, loads for every path.
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Two repos, one branch name feat/task-3.3-quarkus-skills; tests first in each; user authorized the FW-P-17 human edit of .claude in a separate umbrella worktree (2026-10-07).
1. Location: skills and rules are framework files in the umbrella .claude; the code-map leaves are per-repo project facts (P3, 'per-repo map'), so they go in wedding-portal docs/code-maps next to its decision record docs/memory.md, where CI can test them (DoD #1, #2).
2. wedding-portal, test first: a JUnit test in application/src/test (beside ContractPinTest) asserts the eight leaves of AC #2 exist and state their fact, and that docs/memory.md records 'raised in the domain, published by the use case' (18 C-01). See it red.
3. wedding-portal: write the eight leaves from architecture-conventions (§3 A3/CT-11, §4, §5.4, §8.4, 18 C-04/C-12/C-18/C-19/C-27/C-28) and the memory.md entry. Green locally, then CI by SHA on the PR.
4. Umbrella, test first: scripts/test/framework-readiness.test.sh checks AC #1 (no Spring-only API and no project noun in the nine skill bodies; each names its framework only via its code-map leaf or has a Quarkus body), AC #3 (the three rule lines) and AC #4 (glob matching of one main source path per module against the paths: frontmatter, exactly one rule file each; general.md has no globs). See AC #1 and #3 red; AC #4 is a pin (no glob change needed).
5. Umbrella: rewrite the nine skill bodies framework-neutral (Quarkus named in the leaves), edit domain.md:14,16 and platform.md:15. Green locally. The umbrella has no CI: evidence is the local run plus the user's merge.
6. Opus review of both diffs; commits, pushes and PRs only with the user's OK per action.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tests first in both repos. wedding-portal: CodeMapLeavesTest 69d188e (9/9 red, all 9 leaves missing), then the leaves and the memory.md entry a5d9b56 (9/9 green; :application:test 91 green locally). PR https://github.com/Vikteur/wedding-portal/pull/10. Umbrella: scripts/test/framework-readiness.test.sh d779d72 (34 passed, 17 failed: AC #1 and #3), then the skills and rules f95b30f (51/51). PR https://github.com/Vikteur/weddingapp/pull/36 (no CI in the umbrella). Commits, pushes and PRs approved by the user ('Approve all', 2026-10-07).

Evidence after merge (2026-10-07):
- AC #2: wedding-portal CodeMapLeavesTest (9 tests) passed in CI run 37632649856, green on PR #10 head a5d9b56. PR #10 merged as 5b1338c.
- AC #1, #3 and #4: umbrella scripts/test/framework-readiness.test.sh. It ran 34 passed, 17 failed before the edit (AC #1 and #3 red; AC #4 is a pin, no glob change needed), and passes 51/51 on umbrella main 1936c75 (PR #36 merge) after.
- DoD #1: AC #2 is tested in wedding-portal. AC #1, #3 and #4 concern umbrella files that wedding-portal does not hold, so their test lives in the umbrella (as TASK-2.2's tests live in rekord-contract).
- DoD #2: wedding-portal PR CI green by SHA as above. The umbrella has no CI; its evidence is the local run on merged main.
- DoD #3 N/A: no code; ArchUnit arrives with TASK-3.1.
- DoD #4 N/A: no contract change.
- DoD #5 N/A: no STOP item. The .claude edit is the human edit FW-P-17 asks for, authorized by the user ('You can make the edits for 3.3 in a separate worktree').
- DoD #6 N/A, DoD #7 N/A: no operation built.
- DoD #8: the leaves, test data and commits hold no secret or personal data.
- DoD #9 N/A: no schema change.
- DoD #10: docs/memory.md records the domain-events decision and where the leaves live.
- DoD #11: reviewed and merged by the user: wedding-portal PR #10 (5b1338c) and weddingapp PR #36 (1936c75).
- Branches feat/task-3.3-quarkus-skills and their worktrees deleted in both repos after the merges, at the user's request.
- Follow-ups, not created: the skills link more code-map leaves than the eight of AC #2, e.g. domain-events.md, testcontainers.md and bean-config-di.md. The pattern-scanner writes those as code appears.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Nine skills (spring-boot-slice-tests, testcontainers, persistence-repository, resilience4j, spring-caching, exception-to-http, bean-config-di, scheduled-tasks, domain-events) are now Quarkus counterparts with no project nouns, and domain.md:14/16 and platform.md:15 are reworded (umbrella PR #36, merge 1936c75). Eight per-repo code-map leaves under wedding-portal docs/code-maps, plus the domain-events decision in docs/memory.md (wedding-portal PR #10, merge 5b1338c). Tests came first in both repos. CodeMapLeavesTest is green in CI run 37632649856 on a5d9b56. framework-readiness.test.sh passes 51/51 on umbrella main. Rule globs are unchanged and pinned by the test.
<!-- SECTION:FINAL_SUMMARY:END -->
