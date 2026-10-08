---
id: TASK-33.4
title: >-
  P4-E04-T04 rekord-api and rekord-backend archived read-only with nothing
  deleted
status: To Do
assignee: []
created_date: '2026-10-07 07:19'
labels:
  - technical
  - P4
  - stop-production-config
  - stop-delete-history
milestone: m-4
dependencies:
  - TASK-33.3
references:
  - 'docs/rewrite/STATUS.md:279-297'
  - 'docs/rewrite/repo-and-contract-decision.md:269-404'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-33
priority: high
type: task
ordinal: 40404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the POC repositories archived read-only after the release so that the POC ends without losing any of its history.

UD-13.f: after the release rekord-api and rekord-backend are archived read-only as a separate STOP step, and nothing is deleted. The other repositories (rekord-contract, wedding-portal and the three frontends) stay active.

- STOP (human approval in the pull request): production-config, delete-history
- Covers: UD-13.f

Plan item `P4-E04-T04` (technical,P4,stop-production-config,stop-delete-history) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the release accepted and a recorded human approval When the archiving step runs Then rekord-api and rekord-backend are archived on GitHub and refuse pushes.
- [ ] #2 Given the lists of branches, tags and commit SHAs of both repositories recorded before archiving When they are read again afterwards Then they are identical.
- [ ] #3 Given rekord-contract, wedding-portal and the frontend repositories planner, rekord-couple and rekord-dj When the archiving step has run Then none of them is archived and their settings are unchanged.
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
