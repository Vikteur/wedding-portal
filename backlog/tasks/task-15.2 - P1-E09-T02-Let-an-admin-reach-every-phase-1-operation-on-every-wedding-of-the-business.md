---
id: TASK-15.2
title: >-
  P1-E09-T02 Let an admin reach every phase-1 operation on every wedding of the
  business
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.5
  - TASK-10.3
  - TASK-13.2
  - TASK-14.3
  - TASK-9.3
  - TASK-8.2
  - TASK-12.6
references:
  - 'docs/rewrite/STATUS.md:298-320'
  - 'rekord-api/src/main/java/app/rekord/security/AppIdentity.java:39-48'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:49-95'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:55-70'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:118-145'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:168-180'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: feature
ordinal: 10902
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to open and change every wedding, line and task of the business without a team-slot assignment so that I can step in for any planner or DJ.

Deviation UD-14.b: an account holding ADMIN passes every PLANNER and every DJ role check of the phase-1 operations and sees every live wedding of the business without being named on a team slot. In rekord-api the role checks and the visibility rule ask for PLANNER (AppIdentity.java:43-44, WeddingRepository.java:49-95, PlanningResource.java:62, :123, :139, :172), so wedding-portal makes its one role predicate for planner rights true for ADMIN or PLANNER. For an admin: listWeddings lists every live wedding of the business, the five wedding reads answer for every live wedding, getTimeline includes the PLANNER_ONLY lines, listTasks lists every task of the business and updateTask follows the planner rule. getMe still answers role PLANNER for such an account and roles holds the stored roles (UD-14.f); the roles granted on the session serve only the checks. A member holding DJ and ADMIN behaves as an admin everywhere. The admin-only abilities of UD-14.d are P1-E02-T03 to P1-E02-T06.

- STOP (human approval in the pull request): auth-access
- Covers: UD-14.b

Plan item `P1-E09-T02` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a business with weddings A and B, a member holding only ADMIN who is named on no team slot, and a planner When the admin and the planner call listWeddings with status all Then both answers list A and B in the same order (deviation UD-14.b; rekord-api shows a caller without PLANNER only the weddings of its slots).
- [ ] #2 Given wedding A with a PLANNER_ONLY line, a TEAM line and an ALL line When the admin calls getWedding, listWeddingPeople, getWeddingTeam, getWeddingSongLists and getTimeline on A Then each answers 200, and getTimeline holds the 3 lines (UD-14.b).
- [ ] #3 Given a task of the business assigned to a DJ When the admin calls listTasks without parameters and then updateTask on that task with title "Confirm the playlist" and status IN_PROGRESS Then listTasks holds every task of the business and updateTask answers 200 with both fields changed, as for a planner (UD-14.b).
- [ ] #4 Given that admin When the admin calls createWedding, addTimelineItem on A, createTask, createVendor and listMembers Then each answers with the status a planner gets, 201 or 200, and no 403 occurs (UD-14.b).
- [ ] #5 Given a member holding DJ and ADMIN who is named on no slot of A When the member calls getTimeline on A and updateTask on a task assigned to another member Then getTimeline answers 200 with the PLANNER_ONLY line and updateTask answers 200 (UD-14.b).
- [ ] #6 Given the member holding only ADMIN When the member calls getMe Then the answer is 200 with role PLANNER and roles [ADMIN] (UD-14.f).
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
