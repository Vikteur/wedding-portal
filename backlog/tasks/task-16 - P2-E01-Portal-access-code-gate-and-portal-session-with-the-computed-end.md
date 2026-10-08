---
id: TASK-16
title: P2-E01 Portal access-code gate and portal session with the computed end
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - epic
  - P2
milestone: m-2
dependencies:
  - TASK-12
  - TASK-15
references:
  - 'docs/rewrite/STATUS.md:240-256'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:120-137'
  - rekord-api/src/main/java/app/rekord/portal/PortalAttempts.java
  - 'rekord-contract/paths/auth.yaml:160-246'
  - docs/rewrite/analysis/12-couple-portal.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 20100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to open the portal with our access code until 7 days after our wedding so that we enter our music wishes without an account.

Goal: the portal gate under UD-9 and UD-10, a STOP item (authentication). Scope: the operations openPortalSession, getPortalIdentity and closePortalSession (closePortalSession is built test-first, UD-15.d); no stored expiry: the code works while the wedding is not CANCELLED, not deleted, the portal not revoked, and before 00:00 on the 8th day after the current wedding date (UD-9.a); portal sessions keep 14 days idle and 30 days absolute (UD-9.b); an open session ends as soon as a condition fails (UD-9.c), with 401 BAD_LINK for a revoked portal or a deleted wedding (UD-10.a) and 410 LINK_EXPIRED for a CANCELLED wedding or one past the end (UD-10.b); sessions outliving the link (RISK-16); the code-gate lockout that is per portal only (RISK-06), kept as the only limit by UD-18.f; a session that answered 410 works again once every UD-9 condition holds again (UD-18.e). Out of scope: the planner side of the link (P1-E06) and the QR code (UX-11, deferred).

Plan item `P2-E01` (epic,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding dated 2027-06-12 that is not CANCELLED and an active portal link When a partner of the couple calls openPortalSession with the access code before 00:00 on 2027-06-20 Then the answer is 200 and sets the rm_portal cookie.
- [ ] #2 Given an open portal session When the wedding is set to CANCELLED or the clock passes 00:00 on the 8th day after the current wedding date Then the next portal call of that session answers 410 LINK_EXPIRED (deviation UD-9, UD-10.b; rekord-api does not check open sessions).
- [ ] #3 Given an open portal session When the planner revokes the portal or deletes the wedding Then the next portal call of that session answers 401 BAD_LINK (UD-10.a).
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
