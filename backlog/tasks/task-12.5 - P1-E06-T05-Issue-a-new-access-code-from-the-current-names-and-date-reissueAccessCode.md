---
id: TASK-12.5
title: >-
  P1-E06-T05 Issue a new access code from the current names and date
  (reissueAccessCode)
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
  - TASK-12.3
  - TASK-12.4
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:247-251'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:102-137'
  - 'rekord-api/src/main/java/app/rekord/wedding/AccessCode.java:30-66'
  - 'rekord-contract/paths/planner.yaml:355-376'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:323-345'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:108-123'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: feature
ordinal: 10605
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to issue the couple a new access code after the date or a partner's name changed so that the code on their card matches the wedding again.

Builds the operation reissueAccessCode. Oracle: WeddingsResource.reissueAccessCode (WeddingsResource.java:247-251), PortalService.reissueCode and partnerNames (PortalService.java:102-116, :128-137) and AccessCode.issue (AccessCode.java:52-66). The code is the first letter of each of the two PARTNER given names ordered by sort order, after canonicalising, followed by the wedding date as ddMMyyyy. It is refused with 422 VALIDATION_FAILED "Both partners need a first name before a code can be issued." unless the wedding has exactly two PARTNER people whose given names canonicalise to a non-empty text, so a third partner also blocks it (BR-PL-15, BR-CP-11). Otherwise every live portal gets the new code and code_stale false; the tokens, the revoked rows and the open sessions are kept, and the previous code no longer opens the portal. The answer is 200 with the PortalLinks of the live portals. A call answered 200 also writes one song_changes row with the wedding's org_id and wedding_id, source_kind dj, action details, kind null, uid null and summary "reissued the access code" (UD-16.UX-03; the Python POC has no reissue, so the summary text is new, and the user confirmed it, UD-19.h). The operation needs the PLANNER role and finds the wedding as getWeddingPortal does. The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `reissueAccessCode`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-PL-15, BR-CP-11, UD-16.UX-03, UX-03, UD-19.h
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E06-T05` (user-story,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 whose PARTNER Julian was renamed to "Noah" and whose date moved to 2027-07-03, so both live portals carry code EJ12062027 with code_stale true, and an open portal session When a planner calls reissueAccessCode Then the answer is 200 with both links carrying code EN03072027 and code_stale false and their urls unchanged, and the session stays open (BR-CP-11).
- [ ] #2 Given that call When the song_changes rows are read Then exactly one row was added, with source_kind dj, action details, kind null, uid null and summary "reissued the access code" (UD-16.UX-03).
- [ ] #3 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 with its FRIENDS portal revoked When a planner calls reissueAccessCode Then the answer is 200 with only the couple link, carrying code EJ12062027 and code_stale false, and the revoked FRIENDS row keeps its old code, code_stale and revoked_at.
- [ ] #4 Given a wedding with the partners "Émile" and "Øyvind" dated 2027-06-12 When a planner calls reissueAccessCode Then both links carry code EO12062027.
- [ ] #5 Given a wedding with one PARTNER, a wedding with the PARTNER people Emma, Julian and Sam, and a wedding with the PARTNER people Emma and "!!" When a planner calls reissueAccessCode on each Then each answer is 422 VALIDATION_FAILED "Both partners need a first name before a code can be issued." with the errors item {field null, code INVALID_VALUE}, the portals keep their code and code_stale, and no song_changes row is added (BR-PL-15, UD-19.d1).
- [ ] #6 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls reissueAccessCode with each Then each answer is 404 NO_WEDDING "There is no such wedding." and nothing changes.
- [ ] #7 Given that wedding and a member holding only DJ named on its DJ slot as CONFIRMED When the DJ calls reissueAccessCode Then the answer is 403 and no portal, session or song_changes row changes.
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
