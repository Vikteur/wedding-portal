---
id: TASK-15.3
title: P1-E09-T03 Assert the phase-1 access matrix for every caller in one test
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
  - stop-auth-access
  - stop-contract-push
milestone: m-1
dependencies:
  - TASK-15.1
  - TASK-15.2
  - TASK-7.9
  - TASK-8.8
  - TASK-9.6
  - TASK-11.5
  - TASK-12.6
  - TASK-13.4
  - TASK-14.4
  - TASK-7.7
  - TASK-8.7
  - TASK-9.4
  - TASK-10.4
  - TASK-10.5
  - TASK-11.2
  - TASK-11.3
  - TASK-12.2
  - TASK-12.3
  - TASK-12.4
  - TASK-12.5
references:
  - 'docs/rewrite/analysis/10-identity-access.md:153-223'
  - 'docs/rewrite/analysis/10-identity-access.md:287-297'
  - 'docs/rewrite/analysis/10-identity-access.md:316'
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-105'
  - 'rekord-api/src/main/java/app/rekord/HealthResource.java:12-20'
  - 'docs/rewrite/analysis/11-planner-weddings.md:70-71'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:51-65'
  - 'docs/rewrite/STATUS.md:298-320'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10903
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one test that calls every phase-1 operation as every kind of caller so that a missing or wrong role check fails the build.

Adds the access-matrix test over the 45 phase-1 operations and health, following the slice-10 matrix (10-identity-access.md:153-223) with the ADMIN deviations of UD-14. The test holds one row per implemented operationId of the contract's phase-1 paths and fails when an operation has no row. Groups: 4 public operations (login, logout, previewInvite, acceptInvite); 10 operations open to every signed-in account (listWeddings, getWedding, listWeddingPeople, getWeddingTeam, getWeddingSongLists, getTimeline, listTasks, updateTask, getMe, changePassword); 31 planner operations (createWedding, updateWedding, deleteWedding, addWeddingPerson, updatePerson, deletePerson, assignTeamRole, getWeddingPortal, rotateWeddingPortal, revokeWeddingPortal, reissueAccessCode, addTimelineItem, updateTimelineItem, deleteTimelineItem, createTask, deleteTask, the 8 vendor operations, listMembers, updateMember, deleteMember, listInvites, createInvite, revokeInvite and setMemberRoles) (BR-ID-25, BR-ID-27, BR-PL-01). Callers: a request without session, a PORTAL session of the COUPLE and of the FRIENDS portal of wedding W inserted by the test, a member holding only DJ named on no slot, the same DJ named on W's DJ slot as PENCILLED, a member holding only PLANNER, a member holding PLANNER and DJ, and a member holding only ADMIN named on no slot. Every request uses ids of W and of rows of the caller's own business, so the answers show the role rule and not the tenancy rule of P1-E09-T05.

- STOP (human approval in the pull request): auth-access, contract-push
- Covers: BR-ID-17, BR-ID-18, BR-ID-25, BR-ID-27, BR-PL-01, BR-PL-02, BR-ID-46, UD-14.c, UD-14.d, UD-19.g, UD-19.h

Plan item `P1-E09-T03` (technical,P1,stop-auth-access,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the contract's phase-1 operationIds When the matrix test starts Then it finds one row for each of the 45 operations and health, and an operationId without a row fails the test.
- [ ] #2 Given a request without session When it calls each of the 41 guarded operations Then each answer is 401 {"detail":{"code":"NOT_SIGNED_IN","message":"Sign in to continue."}} and no row changes (BR-ID-18).
- [ ] #3 Given a PORTAL session of the COUPLE portal of W and one of the FRIENDS portal of W When each calls each of the 41 guarded operations with ids of W and of rows of W's business Then each answer is 403 and no row changes (BR-PL-02).
- [ ] #4 Given the DJ named on no slot When the DJ calls each of the 31 planner operations Then each answer is 403 with no row changed (BR-ID-25, BR-ID-27, BR-PL-01).
- [ ] #5 Given the DJ named on no slot When the DJ calls getWedding, listWeddingPeople, getWeddingTeam, getWeddingSongLists and getTimeline on W, listWeddings, and listTasks Then the five reads answer 404 NO_WEDDING "There is no such wedding.", listWeddings answers 200 without W, and listTasks answers 200 with only the tasks assigned to the DJ.
- [ ] #6 Given the DJ named on W's DJ slot as PENCILLED and W's run sheet with a PLANNER_ONLY line When the DJ calls the five wedding reads on W, then updateTask with status DONE on a task assigned to another member Then the reads answer 200 with the PLANNER_ONLY line left out of getTimeline, and updateTask answers 403 FORBIDDEN "That task is not yours.".
- [ ] #7 Given the member holding only PLANNER When it calls every guarded operation with a valid body on a member, wedding or row other than itself Then each answer is 200, 201 or 204, except deleteMember, setMemberRoles, updateMember with a status, updateMember with a display_name and createInvite with role ADMIN, which answer 403 FORBIDDEN "This is not yours to open." (UD-14.d, UD-19.g), and the member holding PLANNER and DJ gets the same answers.
- [ ] #8 Given the member holding only ADMIN When it calls every guarded operation with a valid body on a member, wedding or row other than itself Then each answer is 200, 201 or 204 (UD-14.b).
- [ ] #9 Given the four public operations When a request without session calls each with a valid body Then none of them answers 401 or 403, and logout without a cookie answers 200 {"signed_out":false} (BR-ID-17).
- [ ] #10 Given the database container of the test stopped When a request without session calls health Then the answer is 200 {"ok":true} (BR-ID-46).
- [ ] #11 Given the route-guard test of P0-E06-T01 When it runs over the phase-1 resource methods Then every method is guarded or one of the 4 public operations and health, and no phase-1 method carries @PermitAll (BR-ID-17).
- [ ] #12 Given wedding W with people, team slots, portals, song lists, a run sheet and tasks, the vendors, members and invites of its business, and a planner and an admin whose sessions were last used 1 minute ago When each calls every phase-1 GET operation with ids of W and of rows of its business Then every answer is 200 and no row of any table is added, changed or removed by the calls (RISK-12, UD-19.h).
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
