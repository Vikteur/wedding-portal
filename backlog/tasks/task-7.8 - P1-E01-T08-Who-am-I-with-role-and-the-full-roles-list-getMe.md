---
id: TASK-7.8
title: P1-E01-T08 Who am I with role and the full roles list (getMe)
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-contract-push
milestone: m-1
dependencies:
  - TASK-7.1
  - TASK-7.6
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:124-134'
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:236-255'
  - 'rekord-api/src/main/java/app/rekord/security/AppIdentity.java:73-87'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:50-73'
  - 'docs/rewrite/analysis/10-identity-access.md:314'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: feature
ordinal: 10108
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want the app to know who I am and every role I hold so that it shows the screens my roles open.

Builds the operation getMe. Oracle: AuthResource.getMe and meResponse (AuthResource.java:124-134, :241-255): it reads the account and its current ACTIVE roles in the session's business. Deviation UD-14.f: role is PLANNER when the member holds ADMIN or PLANNER and DJ otherwise, and the new roles list (contract change of P1-E01-T01) holds every ACTIVE role sorted by name. getMe needs an account on the session, so a portal session is refused with 403 FORBIDDEN by AppIdentity.requireUser (AppIdentity.java:80-86). Portal sessions are opened from P2 on; the test inserts a PORTAL session row directly.

- Builds: `getMe`
- STOP (human approval in the pull request): auth-access, contract-push
- Covers: BR-ID-44, UD-14.f

Plan item `P1-E01-T08` (user-story,P1,stop-auth-access,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a member holding ADMIN and DJ When the member calls getMe Then the answer is 200 with role PLANNER and roles [ADMIN, DJ] (deviation UD-14.f).
- [ ] #2 Given a member holding DJ and PLANNER, and a member holding only DJ When each calls getMe Then the first gets role PLANNER and roles [DJ, PLANNER], and the second gets role DJ and roles [DJ] (BR-ID-44).
- [ ] #3 Given a planner of the business "Rekord Match" When the planner calls getMe Then the user object holds id, email, display_name, role, roles and organization_name "Rekord Match".
- [ ] #4 Given a request without a session When it calls getMe Then the answer is 401 NOT_SIGNED_IN "Sign in to continue.".
- [ ] #5 Given a request carrying only a live PORTAL session When it calls getMe Then the answer is 403 FORBIDDEN "This is not yours to open." (AppIdentity.requireUser).
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
