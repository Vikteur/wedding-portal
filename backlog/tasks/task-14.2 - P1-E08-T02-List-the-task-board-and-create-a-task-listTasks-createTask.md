---
id: TASK-14.2
title: 'P1-E08-T02 List the task board and create a task (listTasks, createTask)'
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-14.1
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:108-166'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:204-207'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:239-256'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:307-325'
  - 'rekord-api/src/main/resources/db/migration/V7__planning.sql:40-76'
  - 'rekord-contract/components/planner.yaml:692-781'
  - 'rekord-contract/paths/planner.yaml:503-556'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:247-264'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-14
priority: high
type: feature
ordinal: 10802
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to add tasks for the business or for one wedding and see them ordered by due date so that no step of a wedding is forgotten.

Builds the operations listTasks and createTask. Oracle: PlanningResource.listTasks and createTask (PlanningResource.java:108-166) and the mapper task (PlanningResource.java:307-325). listTasks is open to every signed-in member and returns the tasks of the caller's business without deleted_at, ordered by due_date with nulls last, then priority, then title. The optional query parameters weddingId, status and assignee narrow the list; a weddingId is first checked with the visibility rule of BR-PL-03, else 404 NO_WEDDING "There is no such wedding.". A caller without the PLANNER role gets only the tasks whose assignee_user_id is the caller, on any wedding of the business, including weddings the caller is not on (BR-PL-41, BR-ID-26). The contract's description of listTasks says such a caller sees tasks on weddings they are on; wedding-portal follows the oracle and the test djSeesOnlyTheirTasks. createTask needs the PLANNER role and answers 201 with the Task: org_id is the caller's business, created_by the caller, status OPEN and priority 2 unless the body gives them, and a given wedding_id is checked with the same visibility rule, else 404 NO_WEDDING "There is no such wedding." (BR-PL-40, BR-PL-58). A task created with status DONE carries no completed_at (BR-PL-43). Field rules of a planner's task body (PlanningResource.apply, PlanningResource.java:239-256): wedding_id, title, detail and assignee_user_id are replaced by the body, null clearing them; status, priority and visible_to_couple keep their value when null; due_date changes only when it is sent non-blank and is read with java.time.LocalDate.parse, so a due date cannot be cleared (BR-PL-43). Deviations under UD-12, each a 422 VALIDATION_FAILED whose message names every violation and whose errors list has one item per violation with code INVALID_VALUE (UD-19.d1), where rekord-api answers 500: a priority outside 1 to 3 (field priority) and a non-blank due_date that LocalDate.parse rejects (field due_date) (BR-PL-40). A missing or empty title is refused with 422 by the contract's validation, as in rekord-api. assignee_name is the display name of the assignee's account. The check that assignee_user_id names a member of the business, and that a planner's new wedding_id on update names a wedding the caller sees, is P1-E09-T04 (RISK-01, BR-PL-58).

- Builds: `listTasks`, `createTask`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-40, BR-PL-41
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E08-T02` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner of a business When the planner calls createTask with only title "Call the florist" Then the answer is 201 with wedding_id null, detail null, status OPEN, priority 2, due_date null, assignee_user_id and assignee_name null, visible_to_couple false and completed_at null, and the row holds created_by the planner's account and the planner's org_id (BR-PL-40).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and a member holding only DJ with display name "DJ Ray" When a planner calls createTask with wedding_id of that wedding, title "Bring the spare XLR", priority 1, due_date 2027-06-01, assignee_user_id the DJ and visible_to_couple true Then the answer is 201 with those values and assignee_name "DJ Ray".
- [ ] #3 Given a planner When the planner calls createTask with title "Send the invoice" and status DONE Then the answer is 201 with status DONE and completed_at null (BR-PL-43).
- [ ] #4 Given the tasks "Book the band" (due 2027-05-01, priority 3), "Send the song list" (due 2027-05-01, priority 1), "Call the florist" (no due date, priority 1) and "Pay the deposit" (due 2027-04-01, priority 2), and a deleted task When a planner calls listTasks Then the answer is 200 with "Pay the deposit", "Send the song list", "Book the band", "Call the florist" in that order and without the deleted task (BR-PL-40).
- [ ] #5 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 with 2 tasks, 1 task of the business without a wedding, and the DJ "DJ Ray" assigned to one task of the wedding When a planner calls listTasks with weddingId of the wedding, then with status OPEN, then with assignee the DJ Then the answers hold the 2 tasks of the wedding, every task with status OPEN, and the DJ's one task.
- [ ] #6 Given the DJ "DJ Ray" named on no slot of any wedding, a task "Bring the spare XLR" assigned to the DJ on a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027, and a task "Call the florist" assigned to nobody When the DJ calls listTasks Then the answer holds "Bring the spare XLR" and not "Call the florist" (BR-PL-41, BR-ID-26).
- [ ] #7 Given a task of another business whose assignee_user_id the test set directly to a member of the caller's business When a planner of the caller's business calls listTasks Then that task is not in the answer.
- [ ] #8 Given a planner When the planner calls createTask with title "Pay the deposit" and priority 4, then with title "Pay the deposit" and due_date "01-06-2027", then with priority 2 and no title Then the answers are 422 VALIDATION_FAILED with the errors item {field priority, code INVALID_VALUE}, {field due_date, code INVALID_VALUE} and {field title, code REQUIRED} in turn, and no task is stored (deviation UD-12 for priority and due_date, UD-19.d1; rekord-api answers 500, BR-PL-40).
- [ ] #9 Given a planner When the planner calls createTask with title "Pay the deposit" and due_date "" Then the answer is 201 with due_date null.
- [ ] #10 Given an unknown wedding id, a wedding of another business and a wedding with deleted_at set When a planner calls createTask with each as wedding_id and calls listTasks with each as weddingId Then each answer is 404 NO_WEDDING "There is no such wedding." and no task is stored (BR-PL-58).
- [ ] #11 Given the DJ "DJ Ray" When the DJ calls createTask with title "Bring the spare XLR" Then the answer is 403 and no task is stored.
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
