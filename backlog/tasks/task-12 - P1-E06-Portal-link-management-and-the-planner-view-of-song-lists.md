---
id: TASK-12
title: P1-E06 Portal link management and the planner view of song lists
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-10
references:
  - 'docs/rewrite/STATUS.md:240-260'
  - 'docs/rewrite/STATUS.md:337-338'
  - rekord-api/src/main/java/app/rekord/wedding/PortalService.java
  - rekord-api/src/main/java/app/rekord/wedding/AccessCode.java
  - 'rekord-contract/components/planner.yaml:568-572'
  - 'docs/rewrite/analysis/41-domain-model-and-glossary.md:723-734'
  - docs/rewrite/analysis/12-couple-portal.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10600
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to see, rotate, revoke and reissue the couple's portal link and read the couple's song lists so that I control who reaches the portal and know what the couple chose.

Goal: the PortalLink aggregate on the planner side. Scope: the operations getWeddingPortal, rotateWeddingPortal, revokeWeddingPortal (test-first, UD-15.d), reissueAccessCode and getWeddingSongLists; PortalLink as its own aggregate referring to the wedding by id (UD-10.d); expires_at returning the computed end with only the contract's description text changed (UD-9, UD-10.c, a contract push); rotate on a revoked link switching it back on, and a change-log line for each rotate, revoke and reissue (UX-03, RISK-22); the stored PENDING code (RISK-02); tokens and codes stored in clear (RISK-07); the expiry not following the wedding date (RISK-20). Out of scope: the portal gate and sessions (P2-E01) and the planner screens for links (RISK-40, a frontend matter).

Plan item `P1-E06` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a revoked portal link When a planner calls rotateWeddingPortal Then the link is active again with a new address and an access code derived from the current partner names and wedding date, and one new change-log row for the rotate exists (deviation UD-16.UX-03; rekord-api skips revoked rows).
- [ ] #2 Given a wedding dated 2027-06-12 When a planner calls getWeddingPortal Then expires_at is 2027-06-19T22:00:00Z (00:00 on 2027-06-20 in the wedding's time zone Europe/Amsterdam), and after the wedding date moves to 2027-07-03 it is 2027-07-10T22:00:00Z (deviation UD-9, UD-10.c).
- [ ] #3 Given an active portal link When a planner calls reissueAccessCode Then getWeddingPortal shows the new access code and one new change-log row for the reissue exists (UD-16.UX-03).
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
