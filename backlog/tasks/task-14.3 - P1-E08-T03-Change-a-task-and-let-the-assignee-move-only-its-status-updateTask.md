---
id: TASK-14.3
title: >-
  P1-E08-T03 Change a task, and let the assignee move only its status
  (updateTask)
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-14.2
references:
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:168-193'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:218-224'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:239-256'
  - 'rekord-contract/components/planner.yaml:739-772'
  - 'rekord-contract/paths/planner.yaml:557-583'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:226-245'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-14
priority: high
type: feature
ordinal: 10803
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to mark the task the planner gave me as done without being able to change anything else about it so that the planner sees my progress on the board.

Builds the operation updateTask. Oracle: PlanningResource.updateTask (PlanningResource.java:168-193) and requireTask (PlanningResource.java:218-224). The task is found by id within the caller's business and without deleted_at, else 404 NO_TASK "There is no such task.". A caller with the PLANNER role has the body applied by the field rules below. A caller without it must be the task's assignee, else 403 FORBIDDEN "That task is not yours.", and only the status of the body is taken when it is not null; the body is still a TaskInput, so a body without title is refused with 422 (BR-PL-42, BR-ID-26). After either path, a task whose status is DONE and whose completed_at is null gets completed_at now and completed_by the caller, a task already DONE keeps its first completed_at, and any other status clears completed_at and completed_by (BR-PL-43). updated_at is set and the answer is 200 with the Task. Field rules of a planner's task body (PlanningResource.apply, PlanningResource.java:239-256): wedding_id, title, detail and assignee_user_id are replaced by the body, null clearing them; status, priority and visible_to_couple keep their value when null; due_date changes only when it is sent non-blank and is read with java.time.LocalDate.parse, so a due date cannot be cleared (BR-PL-43). Deviations under UD-12, each a 422 VALIDATION_FAILED whose message names every violation and whose errors list has one item per violation with code INVALID_VALUE (UD-19.d1), where rekord-api answers 500: a priority outside 1 to 3 (field priority) and a non-blank due_date that LocalDate.parse rejects (field due_date) (BR-PL-40). A missing or empty title is refused with 422 by the contract's validation, as in rekord-api. assignee_name is the display name of the assignee's account. The check that assignee_user_id names a member of the business, and that a planner's new wedding_id on update names a wedding the caller sees, is P1-E09-T04 (RISK-01, BR-PL-58).

- Builds: `updateTask`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-42, BR-PL-43, BR-ID-26
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E08-T03` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a task "Bring the spare XLR" assigned to the DJ "DJ Ray" with status OPEN When the DJ calls updateTask with title "Something else entirely" and status DONE Then the answer is 200 with status DONE, title "Bring the spare XLR" and completed_at set, and the row holds completed_by the DJ's account (BR-PL-42, BR-ID-26).
- [ ] #2 Given that task When the DJ calls updateTask with status DONE again Then completed_at keeps its first value, and a later call with status IN_PROGRESS answers 200 with completed_at null and the row holds completed_by null (BR-PL-43).
- [ ] #3 Given a task assigned to nobody and a task assigned to another member When the DJ calls updateTask on each with title "Bring the spare XLR" and status DONE Then each answer is 403 FORBIDDEN "That task is not yours." and both tasks are unchanged (BR-PL-42).
- [ ] #4 Given the task assigned to the DJ When the DJ calls updateTask with only status DONE and no title Then the answer is 422 VALIDATION_FAILED with the errors item {field title, code REQUIRED} and the task is unchanged (BR-PL-42, UD-19.d1).
- [ ] #5 Given a task of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 with detail "Two cables", status IN_PROGRESS, priority 1, due_date 2027-06-01, assignee the DJ and visible_to_couple true When a planner calls updateTask with only title "Bring two spare XLR" Then the answer is 200 with wedding_id null, detail null, assignee_user_id and assignee_name null, and title, status IN_PROGRESS, priority 1, due_date 2027-06-01 and visible_to_couple true kept (BR-PL-43).
- [ ] #6 Given that task When a planner calls updateTask with title "Bring two spare XLR" and due_date "", then with due_date 2027-06-05 Then the first answer keeps due_date 2027-06-01 and the second has due_date 2027-06-05 (BR-PL-43).
- [ ] #7 Given that task When a planner calls updateTask with title "Bring two spare XLR" and status DONE Then the answer has completed_at set and the row holds completed_by the planner's account (BR-PL-43).
- [ ] #8 Given that task When a planner calls updateTask with title "Bring two spare XLR" and priority 0, then with title "Bring two spare XLR" and due_date "2027-13-01" Then the answers are 422 VALIDATION_FAILED with the errors item {field priority, code INVALID_VALUE} and {field due_date, code INVALID_VALUE} in turn, and the task is unchanged (deviation UD-12, UD-19.d1; rekord-api answers 500).
- [ ] #9 Given an unknown task id, a task of another business and a task with deleted_at set When a planner calls updateTask with each Then each answer is 404 NO_TASK "There is no such task." and no row changes.
- [ ] #10 Given a task assigned to the DJ "DJ Ray" When the DJ calls updateTask with title "x", status DONE, priority 4 and due_date "01-06-2027" Then the answer is 200 with status DONE and completed_at set, and title, priority and due_date are unchanged, because the assignee path reads only status (PlanningResource.java:176-182).
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
