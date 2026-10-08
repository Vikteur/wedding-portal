---
id: TASK-6.1
title: >-
  P0-E06-T01 Route-guard test over every generated interface with the
  seven-entry public allow-list
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
  - stop-auth-access
milestone: m-0
dependencies:
  - TASK-2.3
  - TASK-5.2
references:
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-137'
  - 'docs/rewrite/architecture-conventions.md:658-744'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: task
ordinal: 601
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want a test that refuses any unguarded route outside the public allow-list so that no later operation ships without an access annotation.

BR-OPS-22: rekord-api's RouteGuardTest requires @Authenticated, @RolesAllowed, @PermitAll or @DenyAll on every generated-interface route outside a public allow-list, a floor of 60 checked routes, a check that every allow-list entry still exists, and 401 on sample guarded routes (rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-137). Phase 0 implements only health, so the floor counts the routes of the generated interfaces and the 401 sample uses a test-only resource. Changing the guard touches authentication, a STOP item.

- STOP (human approval in the pull request): auth-access
- Covers: BR-OPS-22

Plan item `P0-E06-T01` (technical,P0,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the generated app.rekord.api interfaces of the pinned contract When the route-guard test lists every method they declare Then it counts at least 60 routes and fails if it finds fewer (deviation: rekord-api counts the routes of implemented resources at rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:75-97; phase 0 implements only health, so the floor is taken over the interfaces).
- [ ] #2 Given the allow-list HealthApi#health, AuthApi#login, AuthApi#logout, AuthApi#closePortalSession, AuthApi#previewInvite, AuthApi#acceptInvite and AuthApi#openPortalSession When the test runs Then each entry exists in the generated interfaces, and a renamed or removed entry fails the test.
- [ ] #3 Given an implemented resource method outside the allow-list without @Authenticated, @RolesAllowed, @PermitAll or @DenyAll on the implementing method or its class (annotations on the generated interface are not counted) When CI runs the route-guard test Then the test fails and names the method.
- [ ] #4 Given a test-only resource method annotated @Authenticated When a visitor calls it without a session Then the answer is 401 with {"detail":{"code":"NOT_SIGNED_IN","message":"Sign in to continue."}}.
- [ ] #5 Given a visitor without a session When GET /api/health is called Then the answer is 200 {"ok":true}.
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
