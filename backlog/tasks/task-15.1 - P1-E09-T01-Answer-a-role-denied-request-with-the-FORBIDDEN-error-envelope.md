---
id: TASK-15.1
title: P1-E09-T01 Answer a role-denied request with the FORBIDDEN error envelope
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.2
  - TASK-9.3
  - TASK-10.2
  - TASK-14.3
  - TASK-10.3
references:
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:67-73'
  - 'rekord-api/src/main/java/app/rekord/security/AppIdentity.java:73-87'
  - >-
    rekord-api/src/test/java/app/rekord/wedding/WeddingsResourceTest.java:216-226
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:170-180'
  - 'docs/rewrite/analysis/10-identity-access.md:136-141'
  - 'docs/rewrite/analysis/10-identity-access.md:583'
  - 'docs/rewrite/analysis/11-planner-weddings.md:200'
  - 'docs/rewrite/architecture-conventions.md:778-798'
  - 'rekord-contract/components/common.yaml:36-46'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: feature
ordinal: 10901
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want an action my role does not allow to answer with a readable refusal so that the app tells me the page is not mine instead of showing an unknown failure.

rekord-api raises io.quarkus.security.ForbiddenException when a role annotation refuses a caller, and maps only jakarta.ws.rs.ForbiddenException (ErrorMappers.java:67-73), so the framework answers 403, with the framework's default body, which the characterization test records; the only test asserts the status alone (WeddingsResourceTest.java:216-226; BR-ID-19, D-13 at 10-identity-access.md:583). The planner client turns such an answer into UNKNOWN "Request failed (403)". Deviation RISK-18, approved by the user (UD-19.i4) and a STOP item: wedding-portal adds a mapper for io.quarkus.security.ForbiddenException that answers 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}}, the same body that AppIdentity.requireUser and requireOrg produce (AppIdentity.java:73-87, BR-ID-20). The fixture of P0-E06-T02 stays as the record of rekord-api's answer, its test now asserts the envelope, and its criterion that no mapper names the Quarkus type is replaced by this ticket. The message "That task is not yours." of updateTask (PlanningResource.java:178) is a service refusal and keeps its own text.

- STOP (human approval in the pull request): auth-access
- Covers: RISK-18, BR-ID-19, BR-ID-20, PIN-10-0289, PIN-11-0200, PIN-10-0583, PIN-10-0139, PIN-10-0156, UD-19.i4

Plan item `P1-E09-T01` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api started from its repository in a test run and a member holding only DJ When the DJ calls createWedding Then the status, the Content-Type header and the body of the answer are stored as a second fixture beside the listMembers fixture of P0-E06-T02 (PIN-10-0289, PIN-11-0200).
- [ ] #2 Given a member holding only DJ When the DJ calls listMembers, createVendor and createWedding on wedding-portal Then each answer is 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} with Content-Type application/json, and no vendor or wedding is stored (deviation RISK-18, UD-19.i4, PIN-10-0583).
- [ ] #3 Given a PORTAL session row of the COUPLE portal of a wedding, inserted by the test When the session calls createWedding Then the answer is 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} (PIN-10-0156).
- [ ] #4 Given that PORTAL session When it calls getMe and listWeddings Then each answer is 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}}, produced by the account check of the use case as in rekord-api (BR-ID-20, PIN-10-0139).
- [ ] #5 Given the role-denied answers of the criteria above When the contract test validates their bodies Then each body is valid against the Error schema of the contract with code FORBIDDEN from the ErrorCode enum.
- [ ] #6 Given a member holding only DJ and a task of the business assigned to another member When the DJ calls updateTask on that task with status DONE Then the answer is 403 FORBIDDEN "That task is not yours." and the task is unchanged.
- [ ] #7 Given the exception mappers of wedding-portal When the P0-E06-T02 test runs Then it asserts that exactly one mapper names io.quarkus.security.ForbiddenException, and the listMembers and createWedding fixtures stay in the test resources as the record of rekord-api's answer beside the new envelope answer (deviation RISK-18).
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
