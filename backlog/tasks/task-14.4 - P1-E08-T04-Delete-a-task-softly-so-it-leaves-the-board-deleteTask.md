---
id: TASK-14.4
title: P1-E08-T04 Delete a task softly so it leaves the board (deleteTask)
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-14.2
references:
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:195-200'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:218-224'
  - 'rekord-contract/paths/planner.yaml:584-600'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-14
priority: high
type: feature
ordinal: 10804
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to delete a task so that it leaves the board while its row stays for the record.

Builds the operation deleteTask. Oracle: PlanningResource.deleteTask (PlanningResource.java:195-200) with requireTask (PlanningResource.java:218-224). The operation needs the PLANNER role. The task is found as updateTask finds it, else 404 NO_TASK "There is no such task."; deleted_at is set to now and the answer is 204 without a body. The row stays, and every read of tasks leaves out rows with deleted_at set, so a deleted task is gone from listTasks and from updateTask and deleteTask (BR-DM-35, BR-PL-44, PIN-15-0101); the couple's task list of P2-E02-T04 leaves deleted tasks out as well. rekord-api has no test for this operation, so the ticket is test-first (UD-15.d).

- Builds: `deleteTask`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-DM-35, BR-PL-44, PIN-15-0101
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E08-T04` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for deleteTask When a characterization test calls deleteTask against rekord-api for a task, the same task a second time, a task of another business and as a DJ Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given the tasks "Call the florist" and "Pay the deposit" of a business When a planner calls deleteTask for "Call the florist" Then the answer is 204 without a body, listTasks holds only "Pay the deposit", and the row of "Call the florist" still exists with deleted_at set (BR-DM-35, PIN-15-0101).
- [ ] #3 Given that deleted task When a planner calls deleteTask and updateTask with title "Call the florist" on its id Then both answers are 404 NO_TASK "There is no such task." and deleted_at keeps its first value (BR-PL-44).
- [ ] #4 Given a task of another business and an unknown task id When a planner calls deleteTask with each Then each answer is 404 NO_TASK "There is no such task." and the other business's task keeps deleted_at null.
- [ ] #5 Given a task assigned to the DJ "DJ Ray" When the DJ calls deleteTask on it Then the answer is 403 and the task keeps deleted_at null.
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
