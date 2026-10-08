---
id: TASK-6.2
title: >-
  P0-E06-T02 Role-denied 403 answer recorded from rekord-api and asserted before
  any role check exists
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - user-story
  - P0
  - stop-auth-access
milestone: m-0
dependencies:
  - TASK-6.1
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:36'
  - 'rekord-contract/dist/openapi.yaml:999-1003'
  - 'docs/rewrite/architecture-conventions.md:778-798'
  - 'rekord-api/src/main/resources/application.properties:48-50'
  - 'rekord-contract/dist/openapi.yaml:1099'
  - 'rekord-contract/dist/openapi.yaml:185'
  - 'rekord-contract/dist/openapi.yaml:1003'
  - 'rekord-api/src/main/resources/application.properties:83-90'
  - 'rekord-api/src/main/java/app/rekord/account/Bootstrap.java:67-88'
  - 'docs/rewrite/STATUS.md:427-428'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: feature
ordinal: 602
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want a request outside my role refused with the 403 answer rekord-api gives so that the DJ app keeps reacting to it without a change.

io.quarkus.security.ForbiddenException (role denied) has no first-party mapper in rekord-api, so its body is the framework's (PIN-AC-0783; architecture-conventions §11.2). PIN-AC-0739 asks to record it from rekord-api and assert it against the new backend. rekord-api denies a DJ on AccountsResource through its class-level @RolesAllowed for planners (rekord-api/src/main/java/app/rekord/account/AccountsResource.java:36); the contract names the operation listMembers, GET /api/org/members (rekord-contract/dist/openapi.yaml:999-1003). rekord-api runs only locally or in CI for the recording, never on a server (UD-13.a). Its %dev profile reuses the Dev Services database across runs (rekord-api/src/main/resources/application.properties:48-50), so the recording overrides that to start from no rows; the first planner exists only through Bootstrap, from ADMIN_USERNAME and an ADMIN_PASSWORD of at least 12 characters (application.properties:83-90; rekord-api/src/main/java/app/rekord/account/Bootstrap.java:67-88), and createInvite needs a signed-in planner (AccountsResource.java:36). UD-19.i4 approves the deviation RISK-18: a role-denied request answers 403 FORBIDDEN in the error envelope with "This is not yours to open.". P1-E09-T01 adds that mapper; phase 0 records and asserts the framework's body, the rekord-api answer that deviation departs from.

- STOP (human approval in the pull request): auth-access
- Covers: PIN-AC-0739, PIN-AC-0783

Plan item `P0-E06-T02` (user-story,P0,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api run locally with ./mvnw quarkus:dev on a fresh Dev Services database (_DEV_QUARKUS_DATASOURCE_DEVSERVICES_REUSE=false), ADMIN_USERNAME=planner@example.com and a test ADMIN_PASSWORD of 12 or more characters, so that Bootstrap creates that planner, who signs in and invites dj@example.com through createInvite with role DJ, accepted through acceptInvite When dj@example.com signs in and calls listMembers (GET /api/org/members) Then the status, the Content-Type header and the body are stored as application/src/test/resources/fixtures/role-denied-403.json in wedding-portal.
- [ ] #2 Given a test-only resource method annotated @RolesAllowed for planners and a caller holding only the DJ role through @TestSecurity When the caller calls it Then the status, Content-Type and body equal the fixture byte for byte.
- [ ] #3 Given the exception mappers of wedding-portal at the end of phase 0 When they are listed Then none of them names io.quarkus.security.ForbiddenException, so the role-denied body stays the framework's in phase 0, and the mapper UD-19.i4 approves is added in P1-E09-T01.
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
