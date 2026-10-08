---
id: TASK-15.5
title: >-
  P1-E09-T05 Prove that path ids of another business answer not found and change
  nothing
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-15.2
  - TASK-8.7
  - TASK-9.6
  - TASK-11.3
  - TASK-12.5
  - TASK-13.4
  - TASK-14.4
  - TASK-9.4
  - TASK-10.4
  - TASK-10.5
  - TASK-11.2
  - TASK-12.6
references:
  - 'docs/rewrite/analysis/15-data-model-persistence.md:40'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:86-95'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:200-210'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:135-147'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:259-266'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:209-216'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10905
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one test that calls every phase-1 operation with path ids of a second business so that a lookup without the business filter fails the build.

Tenant filtering is done in code, not by the database: every lookup by a path id is filtered on the caller's business or on the owning wedding (BR-DM-07, PIN-15-0062). The build tickets state each refusal; this test asserts them together with business A and business B, each with a wedding, a person, a team slot, a run-sheet line, a task, a vendor with a contact, a member and an open invite. A member of A holding PLANNER and ADMIN, so that the admin-only checks of UD-14.d pass, and in a second pass a member of A holding only ADMIN, calls each of the 30 phase-1 operations with a path id, and listTasks with weddingId, using the ids of B; updateMember is called with display_name only. Answers: the 14 operations on a wedding id and listTasks with weddingId answer 404 NO_WEDDING "There is no such wedding."; updatePerson, deletePerson, updateTimelineItem and deleteTimelineItem answer 404 NO_WEDDING "There is no such wedding." for a person or line of business B, because the row is reached through its wedding (WeddingsResource.java:259-266, PlanningResource.java:209-216; "There is no such person." and "There is no such line." answer only an unknown id); updateTask and deleteTask answer 404 NO_TASK "There is no such task."; getVendor, updateVendor, deleteVendor and addVendorContact answer 404 NO_VENDOR "There is no such vendor."; updateVendorContact and deleteVendorContact answer 404 NO_VENDOR "There is no such contact."; updateMember, deleteMember and setMemberRoles answer 404 NO_USER "There is no such account."; revokeInvite answers 404 INVITE_INVALID "There is no such invitation.".

- STOP (human approval in the pull request): auth-access
- Covers: BR-DM-07, PIN-15-0062

Plan item `P1-E09-T05` (technical,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given businesses A and B as described and a member of A holding PLANNER and ADMIN When the member calls each of the 30 operations with a path id of B Then each answer is the 404 refusal listed for its group in the description, and updatePerson, deletePerson, updateTimelineItem and deleteTimelineItem answer 404 NO_WEDDING "There is no such wedding.".
- [ ] #2 Given the same calls When the test compares every row of B before and after Then no row of B changed, was added or was removed (BR-DM-07, PIN-15-0062).
- [ ] #3 Given a member holding only ADMIN in A When it repeats the 30 calls Then each answer equals the first pass's, because admin reach covers only the admin's own business (UD-14.b).
- [ ] #4 Given the wedding of B When a planner of A without ADMIN calls listTasks with weddingId of that wedding Then the answer is 404 NO_WEDDING "There is no such wedding.".
- [ ] #5 Given the operations of the contract's phase-1 paths When the test starts Then it finds a row for each operation whose path holds an id, and an operation with a path id but without a row fails the test.
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
