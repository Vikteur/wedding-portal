---
id: TASK-12.4
title: P1-E06-T04 Switch a portal link off and end its sessions (revokeWeddingPortal)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - test-first
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-12.2
  - TASK-12.1
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:240-245'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:85-93'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:139-145'
  - 'rekord-contract/paths/planner.yaml:329-353'
  - 'rekord-backend/server/couples_api.py:195-204'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:108-123'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: feature
ordinal: 10604
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to switch the couple's or the friends' link off at once so that nobody gets in through it until I issue a new one.

Builds the operation revokeWeddingPortal. Oracle: WeddingsResource.revokeWeddingPortal (WeddingsResource.java:240-245) and PortalService.revoke (PortalService.java:85-93): for each live portal of the scope (COUPLE, FRIENDS, both when omitted) revoked_at and updated_at are set and every open session of that portal gets revoked_at; the row is kept and nothing replaces it (BR-PL-17, BR-CP-10). A scope already revoked is left as it is. The answer is 200 with the PortalLinks of the live portals that remain. Switching a link back on is rotateWeddingPortal (P1-E06-T03). Each call also writes one song_changes row with the wedding's org_id and wedding_id, source_kind dj, action details, kind null, uid null and summary "revoked the couple link" or "revoked the friends link" per scope of the call, COUPLE first, as the Python POC writes one row per call whatever the link's state (couples_api.py:195-204, UD-16.UX-03). rekord-api has no test for this operation, so the ticket is test-first (UD-15.d). The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `revokeWeddingPortal`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-PL-17, BR-CP-10, UX-03
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E06-T04` (user-story,P1,test-first,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for revokeWeddingPortal When a characterization test calls revokeWeddingPortal against rekord-api with scope FRIENDS, without scope, a second time on a revoked scope and for an unknown wedding id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and an open portal session on each portal When a planner calls revokeWeddingPortal with scope FRIENDS Then the answer is 200 with the couple link and no friends link, the FRIENDS row has revoked_at set and still exists, its session is revoked, and the COUPLE session stays open (BR-PL-17, BR-CP-10).
- [ ] #3 Given that wedding When a planner calls revokeWeddingPortal without scope Then the answer is 200 with no couple link and no friends link, both rows carry revoked_at, and the song_changes rows added are "revoked the couple link" then "revoked the friends link", each with source_kind dj, action details, kind null and uid null (UD-16.UX-03).
- [ ] #4 Given that wedding with its FRIENDS portal already revoked When a planner calls revokeWeddingPortal with scope FRIENDS Then the answer is 200, the FRIENDS row keeps its first revoked_at, and one song_changes row "revoked the friends link" is added.
- [ ] #5 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls revokeWeddingPortal with each Then each answer is 404 NO_WEDDING "There is no such wedding." and no portal, session or song_changes row changes.
- [ ] #6 Given that wedding and a member holding only DJ named on its DJ slot as CONFIRMED When the DJ calls revokeWeddingPortal Then the answer is 403 and no portal, session or song_changes row changes.
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
