---
id: TASK-7
title: 'P1-E01 Accounts, roles, the first admin and sign-in'
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-4
  - TASK-6
references:
  - 'docs/rewrite/STATUS.md:298-320'
  - 'docs/rewrite/STATUS.md:342-344'
  - rekord-api/src/main/java/app/rekord/security/AuthResource.java
  - rekord-api/src/main/java/app/rekord/account/Bootstrap.java
  - rekord-api/src/main/java/app/rekord/security/SessionService.java
  - rekord-contract/paths/auth.yaml
  - docs/rewrite/analysis/10-identity-access.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want to sign in with my e-mail address and password, see who I am with all my roles and change my password so that I reach the apps with the rights my roles give.

Goal: identity for every later operation. Scope: the operations login, logout, getMe and changePassword (logout and changePassword have no rekord-api test and are built test-first, UD-15.d); the role model with ADMIN next to PLANNER and DJ, any combination and at least one role per member (UD-14.a); getMe keeping role and adding the roles list, an additive contract change (UD-14.f); the first admin created at first start from the deployment's secret settings (UD-14.g); the break-glass server command that sets a new password with no in-app reset (UX-12, RISK-44); the sign-in throttle of 5 failures per e-mail address per client within 300 seconds answered with 429 RATE_LIMITED (UX-13, RISK-05); the rm_session cookie and session limits of architecture-conventions §10.2; CSRF defence by SameSite=Lax only, with no Origin check (RISK-12, UD-19.h). Every item touches authentication and is a STOP item. Out of scope: members and invites (P1-E02) and portal sessions (P2-E01).

Plan item `P1-E01` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an empty database and secret settings holding an admin e-mail address and an initial password When wedding-portal starts for the first time Then exactly one account exists, holding only the ADMIN role, together with its business, and login with that address and password answers 200 (UD-14.g).
- [ ] #2 Given a member holding ADMIN and DJ When the member calls getMe Then role is PLANNER and roles lists ADMIN and DJ (deviation UD-14.f).
- [ ] #3 Given 5 failed sign-ins for one e-mail address from one client within 300 seconds When that client calls login for that address again inside the window Then the answer is 429 RATE_LIMITED (deviation UD-16.UX-13).
- [ ] #4 Given an account and the server's break-glass command When the command sets a new password for that account Then login with the new password answers 200 and login with the old password is refused (UD-16.UX-12).
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
