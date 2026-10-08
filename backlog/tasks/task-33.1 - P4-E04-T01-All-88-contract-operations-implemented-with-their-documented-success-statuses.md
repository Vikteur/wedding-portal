---
id: TASK-33.1
title: >-
  P4-E04-T01 All 88 contract operations implemented with their documented
  success statuses
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
milestone: m-4
dependencies:
  - TASK-31
  - TASK-32
references:
  - rekord-contract/dist/openapi.yaml
  - 'docs/rewrite/architecture-conventions.md:495-499'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:64-73'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-33
priority: high
type: task
ordinal: 40401
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want proof that every contract operation is built with its documented success status so that the release serves the whole contract.

UD-15.d: all 88 operations of the released contract (the 84 of the W3 contract, the role operation of UD-14 and the three export operations of UD-16) are built, each in its phase, test-first where rekord-api has no test. PIN-AC-0499: one helper in app.rekord.adapter.web.shared sets the success status from the contract, no resource writes the status by hand, and each operation documented with 201 or 204 answers that status (architecture-conventions §7; rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:64-73).

- Covers: UD-15.d, PIN-AC-0499

Plan item `P4-E04-T01` (technical,P4) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the generated interfaces of the released contract tag When the release candidate is checked Then every operationId of that tag (the 84 of the W3 contract, the role operation of UD-14 and the three export operations of UD-16) has an implementing resource method, and none answers 501 or throws a not-implemented exception.
- [ ] #2 Given the operations the contract documents with 201 or 204 When their tests run Then each answers that status, and no resource method sets a success status other than through the helper in app.rekord.adapter.web.shared.
- [ ] #3 Given the operations whose tickets carry testFirst true in the merged backlog plan When CI runs on the release commit SHA Then the characterization test of each passes in that run.
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
