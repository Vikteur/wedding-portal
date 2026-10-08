---
id: TASK-6
title: >-
  P0-E06 Security foundation: route guard, public allow-list, CORS and
  token-free logs
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - epic
  - P0
milestone: m-0
dependencies:
  - TASK-5
references:
  - 'docs/rewrite/architecture-conventions.md:658-744'
  - 'docs/rewrite/architecture-conventions.md:799-861'
  - rekord-api/src/main/resources/application.properties
  - rekord-api/src/main/java/app/rekord/security/SessionCookieMechanism.java
  - docs/rewrite/backlog-plan/digest-P0.md
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:180-196'
  - 'docs/rewrite/STATUS.md:409-411'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 600
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a visitor, I want every non-public route to refuse me without a session and no access token to appear in a log so that wedding data stays private.

Goal: the security baseline every operation inherits. Scope: every generated route outside the public allow-list carries an authentication or role annotation, checked by a test (BR-OPS-22); no setting turns authentication off (RISK-03); CORS off outside dev and limited to the configured dev origins in dev (BR-OPS-24); the apps call /api on their own origin (BR-OPS-18); the role-denied 403 body recorded from rekord-api (architecture-conventions §10.3); the masking helper of the logging module and the UD-19.f rule: no log line holds a token of any kind (invite, portal, guest, session), an access code, a password, an e-mail address, a person's name or a phone number, log lines identify rows by internal ids only, and LogSafe.token and LogSafe.email write the fixed text [redacted] (architecture-conventions §12.1); RISK-13 as a whole, proxy logs included, is settled in P4-E01-T05 and checked again on the new server in P4-E04-T02. Out of scope: sign-in, sessions and roles (P1-E01), the access matrix of the planner operations (P1-E09), portal sessions (P2-E01), and the cross-backend cookie test, superseded by UD-13.

Plan item `P0-E06` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the resource classes implemented in wedding-portal When the route-guard test walks every method they implement from the generated app.rekord.api interfaces Then each method outside the public allow-list (health, login, logout, previewInvite, acceptInvite, openPortalSession, closePortalSession) carries @Authenticated, @RolesAllowed, @PermitAll or @DenyAll on the implementing method or its class, and an unannotated new resource method fails CI.
- [ ] #2 Given a request whose path or body carries a portal token, an invite token or an access code When the request is served with the log category app.rekord at TRACE and every other category at DEBUG Then no captured log line holds the token or the code, and a line that names a token holds [redacted] in its place (UD-19.f).
- [ ] #3 Given the application outside the dev profile When a browser sends a cross-origin request with credentials Then the answer carries no CORS header.
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
