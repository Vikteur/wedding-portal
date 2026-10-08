---
id: TASK-12.3
title: >-
  P1-E06-T03 Rotate a portal link, switching a revoked link back on
  (rotateWeddingPortal)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-12.2
  - TASK-12.1
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:231-238'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:52-83'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:118-137'
  - 'rekord-api/src/main/java/app/rekord/wedding/AccessCode.java:52-66'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:139-145'
  - 'rekord-contract/paths/planner.yaml:303-327'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:323-345'
  - 'rekord-api/src/test/java/app/rekord/portal/PortalTest.java:161-180'
  - 'rekord-backend/server/couples_api.py:184-192'
  - 'rekord-backend/server/couples.py:267-275'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:108-123'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: feature
ordinal: 10603
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to issue a fresh link for the couple or the friends, also after I switched it off, so that a forwarded link stops working and access can be given back.

Builds the operation rotateWeddingPortal. Oracle: WeddingsResource.rotateWeddingPortal (WeddingsResource.java:231-238) and PortalService.rotate (PortalService.java:52-83). The scope query parameter is COUPLE or FRIENDS, and both when omitted. For each scope whose portal is live, as in rekord-api: the live row gets revoked_at, every open session of that portal gets revoked_at, the change is flushed, and a new portal row is inserted with a new token, the same access code and code_stale, rotated_from set to the old row's id and created_by set to the caller (BR-PL-16, BR-CP-09). Deviation for a scope whose portal is revoked (UD-16.UX-03, UX-03, RISK-22; rekord-api skips it, BR-DM-20, PIN-15-0081): a new portal row is inserted with a new token and a code derived with AccessCode.issue from the current PARTNER given names ordered by sort order and the current wedding date (AccessCode.java:52-66), code_stale false; when no code can be derived it stores no code with code_stale true, shown as PENDING (RISK-02, PIN-15-0084); rotated_from stays null, because no rekord-api code reads rotated_from. Deviation UD-19.i6 (BR-CP-08): when the other scope's portal is live, the revived portal takes that live portal's code and code_stale flag instead of a derived code, so both portals keep one code; a code is derived only when no portal of the wedding is live. The revoked rows are kept. The answer is 200 with the PortalLinks of every live portal of the wedding, whatever the scope. Each call also writes one song_changes row with the wedding's org_id and wedding_id, source_kind dj, action details, kind null, uid null and summary "rotated the couple link" or "rotated the friends link" per scope it acts on, COUPLE first (UD-16.UX-03, as the Python POC writes it, couples_api.py:184-192). The operation needs the PLANNER role and finds the wedding as getWeddingPortal does. The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `rotateWeddingPortal`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-PL-16, BR-CP-09, BR-DM-20, PIN-15-0081, PIN-20-0123, UX-03, RISK-22, PIN-15-0084, UD-19.i6
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E06-T03` (user-story,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and an open portal session on each portal When a planner calls rotateWeddingPortal with scope FRIENDS Then the answer is 200 with the couple link unchanged and a friends link with a new token, code EJ12062027 and code_stale false, the old FRIENDS row has revoked_at set and is the new row's rotated_from, the FRIENDS session is revoked and the COUPLE session stays open (BR-PL-16, BR-CP-09).
- [ ] #2 Given that wedding with code_stale true on both live portals When a planner calls rotateWeddingPortal without scope Then both links have new tokens, both keep code EJ12062027 with code_stale true, and the song_changes rows added are "rotated the couple link" then "rotated the friends link", each with source_kind dj, action details, kind null and uid null (PIN-15-0084, UD-16.UX-03).
- [ ] #3 Given that wedding with its FRIENDS portal's revoked_at set directly by the test When a planner calls rotateWeddingPortal with scope FRIENDS Then the answer holds a live friends link with a new token, code EJ12062027, code_stale false and rotated_from null, and the revoked FRIENDS rows are kept (deviation UD-16.UX-03, RISK-22, PIN-20-0123; rekord-api returns no friends link, PIN-15-0081).
- [ ] #4 Given that wedding with both portals revoked, the PARTNER Julian renamed to "Noah" and the date moved to 2027-07-03 When a planner calls rotateWeddingPortal without scope Then both links are live again with new tokens and code EN03072027 with code_stale false, and two song_changes rows are added (deviation UD-16.UX-03; rekord-api answers no link, BR-DM-20).
- [ ] #5 Given a wedding with one PARTNER and both portals revoked When a planner calls rotateWeddingPortal with scope COUPLE Then the couple link is live with a new token, no stored code, code PENDING and code_stale true (RISK-02, PIN-15-0084).
- [ ] #6 Given a rotate call When the test makes the insert of the new portal fail after the old row was revoked Then the call answers 500 UNKNOWN and the old portal, its sessions and the song_changes rows are unchanged.
- [ ] #7 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls rotateWeddingPortal with each Then each answer is 404 NO_WEDDING "There is no such wedding." and no portal, session or song_changes row changes.
- [ ] #8 Given that wedding and a member holding only DJ named on its DJ slot as CONFIRMED When the DJ calls rotateWeddingPortal Then the answer is 403 and no portal, session or song_changes row changes.
- [ ] #9 Given that wedding with its FRIENDS portal's revoked_at set directly by the test and the PARTNER Julian then renamed to "Noah", so the live COUPLE portal carries code EJ12062027 with code_stale true When a planner calls rotateWeddingPortal with scope FRIENDS Then the answer holds a live friends link with a new token, code EJ12062027 and code_stale true, and the couple link is unchanged (deviation UD-19.i6, BR-CP-08).
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
