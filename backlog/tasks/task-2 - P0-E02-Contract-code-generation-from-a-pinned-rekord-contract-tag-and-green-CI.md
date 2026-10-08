---
id: TASK-2
title: P0-E02 Contract code generation from a pinned rekord-contract tag and green CI
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - epic
  - P0
milestone: m-0
dependencies:
  - TASK-1
references:
  - 'docs/rewrite/STATUS.md:321-329'
  - 'docs/rewrite/architecture-conventions.md:232-318'
  - 'docs/rewrite/architecture-conventions.md:942-1002'
  - 'docs/rewrite/repo-and-contract-decision.md:866-943'
  - rekord-contract/openapi.yaml
  - docs/rewrite/backlog-plan/digest-P0.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want wedding-portal to generate its server interfaces from a pinned contract tag in a CI run that reads the private contract with a read-only token so that every contract change reaches the backend through a tag and a green build.

Goal: contract-first from rekord-contract with CI green from phase 0 on (UD-13.e). Scope: one contract hub authored in files per audience (UD-15.b); a tag on every merge to the contract's main, consumers pinning a tag, and one feature-branch name across every involved repository that pins the contract's feature branch until it merges (UD-15.c); info.version always equal to the tag, bumped by the author in the pull request and checked by CI, the hub workflow tagging v<info.version> after the merge, nothing but the merge pushing to main, and v0.1.0 as the first tag (UD-19.e, UD-20.a, UD-20.b); the openApiGenerate task with the options of architecture-conventions §4 and its output under build/generated so the generated-code guard covers it; one fine-grained read-only token stored as a CI secret (UD-15.e); the CI of wedding-portal on every push and pull request, read by commit SHA (BR-OPS-29); the contract CI rules for the committed bundle, the lint policy and the breaking-change gate with a pinned action (BR-OPS-03, BR-OPS-05, BR-OPS-06); the red cross-repo CI (RISK-51) and the unpinned contract (RISK-57). Out of scope: deploy workflows and their host keys (P4), and the UD-12 contract change itself (P0-E05).

Plan item `P0-E02` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a merge to the main branch of rekord-contract When the merge completes Then the tag v<info.version> of that merge commit's openapi.yaml exists on it (UD-20.b), the build file of wedding-portal on main names a rekord-contract tag, and on a feature branch it names the rekord-contract feature branch of the same name until that branch merges (UD-15.c).
- [ ] #2 Given a push to a wedding-portal branch When CI runs Then the run checks out the private contract with the read-only token stored as a CI secret, generates the server interfaces under build/generated, compiles them and reports its result, which is read by the commit SHA.
- [ ] #3 Given a contract pull request that removes an operation When the contract CI runs Then the breaking-change gate fails the pull request unless it carries the label semver:major, with which the gate passes and the merge raises the major part of the tag (UD-19.e).
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [ ] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [ ] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [ ] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [ ] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [ ] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [ ] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [ ] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [ ] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [ ] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [ ] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->
