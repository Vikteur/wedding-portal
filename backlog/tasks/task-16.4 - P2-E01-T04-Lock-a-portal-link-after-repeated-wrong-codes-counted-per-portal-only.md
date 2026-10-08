---
id: TASK-16.4
title: >-
  P2-E01-T04 Lock a portal link after repeated wrong codes, counted per portal
  only
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-16.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalAttempts.java:28-61'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:61-111'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:85-93'
  - 'rekord-api/src/main/resources/db/migration/V5__portals.sql:49-52'
  - 'rekord-api/src/test/java/app/rekord/portal/PortalTest.java:182-195'
  - 'docs/rewrite/analysis/12-couple-portal.md:74-80'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-16
priority: high
type: feature
ordinal: 20104
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want our portal link to lock after repeated wrong codes so that nobody guesses our access code.

The lockout of the code gate, an authentication STOP item. Oracle: PortalAttempts (PortalAttempts.java:28-61) and PortalGate.check (PortalGate.java:76-104): after the 5th consecutive miss the portal locks for 15 minutes, doubling with each further miss up to 360 minutes; the lock is checked before anything else, so even the right code gets 429 CODE_LOCKED; a right code resets the count; misses never decay with time; a wrong code on a revoked or expired link counts nothing; the count is written in its own transaction, so the refusal that follows cannot roll it back. UD-18.f keeps this per-portal lockout as the only limit, with no per-client or global throttle (RISK-06, slice 12 R3). P2-E01-T01 counts the misses; rekord-api tests the first lock only (PortalTest.java:182-195), so the escalation, the reset and the check order are pinned here.

- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-04, BR-ID-37, BR-ID-47, PIN-10-0317, RISK-06, UD-18.f

Plan item `P2-E01-T04` (user-story,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027, and its COUPLE portal with no misses at 10:00 on 2027-06-01 in Europe/Amsterdam, the time zone of every clock time in this criterion, When a visitor sends 5 wrong codes in a row to openPortalSession, waits out each lock and sends one further wrong code at a time up to the 12th miss Then every miss answers 401 BAD_LINK, the 5th miss locks the portal for 15 minutes, the 6th to 10th misses lock it for 30, 60, 120, 240 and 360 minutes, and the 11th and 12th misses lock it for 360 minutes.
- [ ] #2 Given the COUPLE portal of the same wedding locked at 10:00 in Europe/Amsterdam, the time zone of every clock time in this criterion, for 15 minutes by its 5th miss When a visitor calls openPortalSession with EJ12062027 at 10:05, with XX00000000 at 10:10 and with EJ12062027 at 10:15 Then the first two answer 429 CODE_LOCKED "Too many tries. Wait a few minutes and try again." without counting a miss, and the third answers 200 and sets the miss count to 0.
- [ ] #3 Given the COUPLE portal of the same wedding with 4 misses and no lock When a visitor calls openPortalSession on 2027-06-01 with EJ12062027 and afterwards sends 4 wrong codes Then the right code answers 200 and sets the miss count to 0, and after the 4 wrong codes the count is 4 and the portal is not locked.
- [ ] #4 Given a CONFIRMED wedding dated 2027-12-31 whose COUPLE portal recorded 4 misses at 10:00 on 2027-01-04 in Europe/Amsterdam, the time zone of every clock time in this criterion, When a visitor sends a 5th wrong code to openPortalSession at 10:00 on 2027-02-03 Then the answer is 401 BAD_LINK and the portal is locked until 10:15 on 2027-02-03, because misses never decay with time alone.
- [ ] #5 Given the COUPLE portal of the same wedding revoked through revokeWeddingPortal with no misses recorded When a visitor sends 6 wrong codes XX00000000 to openPortalSession with the revoked token Then all 6 answers are 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you.", none answers 429 CODE_LOCKED, and the portal's failed-attempt count is still 0 (BR-ID-47, PIN-10-0317).
- [ ] #6 Given the COUPLE portal of the same wedding locked at 10:00 in Europe/Amsterdam, the time zone of every clock time in this criterion, for 15 minutes by its 5th miss and revoked through revokeWeddingPortal at 10:02 When a visitor calls openPortalSession with the revoked token and EJ12062027 at 10:05 and at 10:15 Then the first answers 429 CODE_LOCKED and the second answers 410 LINK_REVOKED "This link was replaced. Ask your DJ for the new one.", because the lock is checked before the revoked state (BR-ID-47).
- [ ] #7 Given the COUPLE portal of the same wedding revoked through revokeWeddingPortal and the wedding set to CANCELLED When a visitor calls openPortalSession with the old token and EJ12062027 Then the answer is 410 LINK_REVOKED "This link was replaced. Ask your DJ for the new one.", because the revoked check comes before the CANCELLED check.
- [ ] #8 Given the COUPLE and FRIENDS portals of the same wedding with no misses When one client sends 5 wrong codes to openPortalSession with the FRIENDS token and then EJ12062027 with the COUPLE token Then the 5th wrong code locks only the FRIENDS portal and the COUPLE answer is 200, because the lockout counts per portal and there is no per-client or global throttle (UD-18.f).
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
