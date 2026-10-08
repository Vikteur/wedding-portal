---
id: TASK-15.4
title: >-
  P1-E09-T04 Refuse body ids of another business and resolve labels only inside
  the business
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.6
  - TASK-9.7
  - TASK-10.4
  - TASK-11.6
  - TASK-13.3
  - TASK-14.3
references:
  - 'docs/rewrite/analysis/15-data-model-persistence.md:421'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:149-166'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:226-237'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:286-305'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:320-323'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:212-215'
  - 'rekord-api/src/main/java/app/rekord/account/InviteService.java:138-147'
  - 'rekord-contract/components/planner.yaml:503'
  - 'docs/rewrite/analysis/11-planner-weddings.md:166'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: feature
ordinal: 10904
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want every id I send in a body checked against my business so that no task, line, wedding or invitation of mine points into another business and no name of another business shows in my screens.

rekord-api stores foreign-key ids from request bodies without checking that the row belongs to the caller's business or to the same wedding; the database keys check only existence (BR-DM-46, 15 R-24 at 15-data-model-persistence.md:421). A planner's updateTask writes wedding_id unchecked, so a task can move onto a wedding of another business and show on that couple's portal (BR-PL-58, PIN-11-0412, RISK-01). Deviation RISK-01: a task's wedding_id on createTask and updateTask is checked with the visibility rule of BR-PL-03, else 404 NO_WEDDING "There is no such wedding.", as createTask does in rekord-api (PlanningResource.java:149-166). Every other body id answers 422 VALIDATION_FAILED with the errors item {field <name>, code INVALID_VALUE} when it does not belong to the business, and an id that names no row gets the identical answer (deviation UD-19.i2; rekord-api answers 500 UNKNOWN on the foreign key): venue_vendor_id on createWedding and updateWedding names a vendor of the business, archived ones included; owner_team_id names a team slot and owner_person_id a person of the line's own wedding on addTimelineItem and updateTimelineItem (BR-PL-38, BR-DM-29); assignee_user_id on createTask and updateTask names an account with a membership in the business; vendor_id on createInvite names a vendor of the business, archived ones included. The check is decided (41-domain-model D-4, architecture-conventions §6.1); the 422 is the answer the user chose for a body id, as in P1-E05-T06 (UD-19.i2), while a task's wedding_id keeps the 404 NO_WEDDING that rekord-api's createTask answers. Labels (deviation RISK-10): owner_label is resolved only from the line's own wedding and the business's vendors (PlanningResource.java:286-305); assignee_name stays the account's display name, since an account belongs to no single business and the write check admits only members (PlanningResource.java:320-323); the vendor_id of listMembers comes only from contacts of the business's vendors (AccountsResource.java:212-215); the contact link at invite acceptance stays limited to the invite's vendor, now checked (InviteService.java:138-147, P1-E03-T07). The team-slot ids are P1-E05-T06.

- STOP (human approval in the pull request): auth-access
- Covers: RISK-01, RISK-10, BR-DM-46, PIN-15-0069, BR-DM-29, PIN-15-0090, BR-PL-38, BR-PL-58, PIN-11-0412, UD-19.i2

Plan item `P1-E09-T04` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner of business A and a wedding of business B When the planner calls createTask with that wedding_id, and updateTask on a task of A with that wedding_id Then both answer 404 NO_WEDDING "There is no such wedding.", no task is stored and the task of A keeps its wedding (deviation RISK-01, BR-PL-58, PIN-11-0412).
- [ ] #2 Given a task of A and a wedding of A with deleted_at set When the planner calls updateTask with that wedding_id Then the answer is 404 NO_WEDDING "There is no such wedding." and the task is unchanged (BR-PL-58).
- [ ] #3 Given a vendor of B and an archived vendor of A When the planner calls createWedding with venue_vendor_id of the vendor of B, then with the archived vendor of A Then the first answers 422 VALIDATION_FAILED with the errors item {field venue_vendor_id, code INVALID_VALUE} with no wedding stored, and the second answers 201 (deviation RISK-01, BR-DM-46, UD-19.i2).
- [ ] #4 Given wedding W1 of A with a team slot and a person, a second wedding W2 of A, and a person of B When the planner calls addTimelineItem on W2 with owner_team_id of W1's slot, with owner_person_id of W1's person, and with owner_person_id of B's person Then each answer is 422 VALIDATION_FAILED with the errors item {field owner_team_id, code INVALID_VALUE}, {field owner_person_id, code INVALID_VALUE} and {field owner_person_id, code INVALID_VALUE} in turn and W2's run sheet is unchanged (BR-PL-38, BR-DM-29, PIN-15-0090, UD-19.i2).
- [ ] #5 Given an account with a membership only in B When the planner calls createTask with assignee_user_id of that account, and updateTask on a task of A with it Then both answer 422 VALIDATION_FAILED with the errors item {field assignee_user_id, code INVALID_VALUE} and nothing is stored or changed (deviation RISK-01, UD-19.i2).
- [ ] #6 Given a vendor of B When the planner calls createInvite for new.dj@example.com with that vendor_id Then the answer is 422 VALIDATION_FAILED with the errors item {field vendor_id, code INVALID_VALUE} and no invite is stored (deviation RISK-01, PIN-15-0069, UD-19.i2).
- [ ] #7 Given a line of W1 whose owner_person_id the test set directly to a person of B, and a task of A whose assignee the test set directly to an account with no membership in A When the planner calls getTimeline on W1 and listTasks Then the line's owner_label is null and the task's assignee_name is the account's display name (deviation RISK-10).
- [ ] #8 Given a member of A whose user id the test set directly as user_id on a contact of a vendor of B When the planner calls listMembers Then that member's vendor_id is null, and a member who is a contact of a vendor of A carries that vendor's id (deviation RISK-10).
- [ ] #9 Given a wedding W1 of A and five random UUIDs that name no row When the planner calls createWedding with one as venue_vendor_id, addTimelineItem on W1 with one as owner_team_id and one as owner_person_id, createTask with one as assignee_user_id, and createInvite for new.dj@example.com with one as vendor_id Then each answer is 422 VALIDATION_FAILED with the one errors item {field <the field sent>, code INVALID_VALUE}, identical to the answer for an id of B, and nothing is stored (deviation UD-19.i2; rekord-api answers 500 UNKNOWN on the foreign key).
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
