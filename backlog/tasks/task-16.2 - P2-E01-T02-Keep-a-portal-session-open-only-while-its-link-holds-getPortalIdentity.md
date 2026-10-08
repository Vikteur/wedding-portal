---
id: TASK-16.2
title: >-
  P2-E01-T02 Keep a portal session open only while its link holds
  (getPortalIdentity)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
  - stop-crypto-logging
milestone: m-2
dependencies:
  - TASK-16.1
  - TASK-12
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:120-137'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:31-110'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:137-145'
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:204-237'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:221-240'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:276-283'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:52-93'
  - >-
    rekord-api/src/main/java/app/rekord/security/SessionCookieMechanism.java:65-72
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:59-65'
  - 'docs/rewrite/analysis/12-couple-portal.md:79'
  - 'docs/rewrite/analysis/12-couple-portal.md:519'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-16
priority: high
type: feature
ordinal: 20102
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want our portal session to stay open across visits until our link ends so that we do not type the code each time.

The portal session and the check every later portal call makes, an authentication STOP item. Oracle: SessionService (portal idle 14 days, absolute 30 days, touched at most hourly), the rm_portal cookie of AuthResource and PortalGate.requireSession (PortalGate.java:120-137), which checks the revoked portal, the path token and the deleted wedding but never the expiry; getPortalIdentity calls it without a path token. BR-ID-38 is kept except its last clause: link expiry is re-checked on every session call (UD-9.c, UD-10.b). UD-9.c and UD-10 make every open session end as soon as a UD-9 condition fails: 410 LINK_EXPIRED for a CANCELLED wedding or a wedding past the computed end, 401 BAD_LINK for a revoked portal or a deleted wedding; for an open session the revoked portal or deleted wedding (401 BAD_LINK) is checked before CANCELLED or past the end (410 LINK_EXPIRED); the path-token check (401 BAD_LINK) comes before the 410 LINK_EXPIRED check, and the 410 check comes before the couple-only check (403 FORBIDDEN). rekord-api ends the sessions of a rotated or revoked portal (PortalService.java:52-93), so it answers 401 NOT_SIGNED_IN there. P1-E06-T03 and P1-E06-T04 set revoked_at on those sessions; wedding-portal still finds the session row by the SHA-256 digest of the rm_portal cookie when its revoked_at is set or its idle or absolute end has passed, answers 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you." when that row's portal has revoked_at set, and answers 401 NOT_SIGNED_IN "Sign in to continue." for a session ended by closePortalSession or by its lifetime on a live portal (UD-10.a). UD-18.e: the conditions are evaluated on every request and nothing stores an ended state, so a session that answered 410 works again when the wedding is no longer CANCELLED or the date moves later, within its own 14-day and 30-day limits. getPortalIdentity has a rekord-api test, so it is not built test-first. Only the rm_portal cookie counts on portal routes, so an rm_session cookie is never read there (UD-19.l3; P2-E05-T01 proves it over every portal operation).

- Builds: `getPortalIdentity`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: UD-9.b, UD-9.c, UD-10.a, UD-10.b, RISK-16, BR-CP-06, PIN-12-0519, BR-ID-38, UD-18.e, UD-19.l3

Plan item `P2-E01-T02` (user-story,P2,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a portal session opened with the COUPLE token of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When the partner calls getPortalIdentity at 12:00 on 2027-06-02 and at 12:00 on 2027-06-14 Then both answers are 200 with scope COUPLE, the wedding_id, names "Emma & Julian" and wedding_date 2027-06-12, with days_until 10 and -2, counted in the wedding time zone.
- [ ] #2 Given no cookie, and in a second browser only the live rm_session cookie of a signed-in planner When getPortalIdentity is called from each Then both answer 401 NOT_SIGNED_IN "Sign in to continue.", because on /api/portal/* only the rm_portal cookie counts (deviation UD-19.l3; rekord-api answers the second 401 NOT_SIGNED_IN "Open the full link your DJ sent you, then enter your code.").
- [ ] #3 Given one openPortalSession with the header X-Forwarded-Proto: https and one without it When the Set-Cookie headers and the sessions table are read Then the cookie is rm_portal with a 43-character base64url value, HttpOnly, SameSite=Lax, Path=/api and Max-Age 2592000, Secure only on the answer to the request with X-Forwarded-Proto: https, and the stored session holds only the SHA-256 digest of that value with the portal, the wedding, the business, the role COUPLE and no user.
- [ ] #4 Given a portal session opened at 10:00 on 2027-01-04 in Europe/Amsterdam, the time zone of every clock time in this criterion, for a wedding dated 2027-12-31 When it calls getPortalIdentity at 10:30 and at 12:00 that day and next at 12:01 on 2027-01-18 Then the 10:30 call leaves last_seen_at at 10:00, the 12:00 call moves last_seen_at to 12:00 and the idle end to 12:00 on 2027-01-18, and the 12:01 call on 2027-01-18 answers 401 NOT_SIGNED_IN "Sign in to continue.".
- [ ] #5 Given a portal session opened at 10:00 on 2027-01-04 in Europe/Amsterdam, the time zone of every clock time in this criterion, for a wedding dated 2027-12-31 that calls getPortalIdentity at 10:00 on every day from 2027-01-05 to 2027-02-02 When it calls getPortalIdentity at 09:59 and at 10:00 on 2027-02-03 Then the 09:59 call answers 200, the 10:00 call answers 401 NOT_SIGNED_IN "Sign in to continue." because the 30-day absolute end has passed, and a new openPortalSession with the right code answers 200.
- [ ] #6 Given a portal session opened at 12:00 on 2027-06-10 for the wedding dated 2027-06-12 in Europe/Amsterdam When it calls getPortalIdentity at 23:59 on 2027-06-19 and at 00:00 on 2027-06-20, and at 12:00 on 2027-06-21 after the planner moved the date to 2027-07-03 through updateWedding Then the first answers 200, the second answers 410 LINK_EXPIRED "This link retired after the wedding." and the third answers 200 (deviation UD-9.c, UD-10.b and UD-18.e; rekord-api answers 200 until the session itself ends).
- [ ] #7 Given a portal session opened at 12:00 on 2027-06-15 for the wedding dated 2027-06-12 in Europe/Amsterdam, whose planner moves the date to 2027-07-03 through updateWedding on 2027-06-16 When the session calls getPortalIdentity at 12:00 on 2027-06-25, at 12:00 on 2027-07-05 and at 00:00 on 2027-07-11 Then the first two answer 200 with days_until 8 and -2 and the third answers 410 LINK_EXPIRED "This link retired after the wedding." (deviation UD-9.a; rekord-api keeps the end stored at issue).
- [ ] #8 Given a portal session opened on 2027-06-01 for the same wedding When the planner sets the wedding to CANCELLED through updateWedding, the session calls getPortalIdentity and a visitor calls openPortalSession with EJ12062027, and then the planner sets the wedding back to CONFIRMED and the session calls getPortalIdentity again Then the first two answers are 410 LINK_EXPIRED "This link retired after the wedding." and the last is 200 (deviation UD-10.b and UD-18.e; rekord-api answers 200 to all three).
- [ ] #9 Given portal sessions opened on 2027-06-01 with the COUPLE tokens of three CONFIRMED weddings dated 2027-06-12 When the planner calls revokeWeddingPortal with scope COUPLE on the first, rotateWeddingPortal with scope COUPLE on the second and deleteWedding without purge on the third, and each session calls getPortalIdentity Then all 3 answers are 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you.", although P1-E06-T04 and P1-E06-T03 set revoked_at on the session rows of the first two (deviation UD-10.a for the first two, where rekord-api answers 401 NOT_SIGNED_IN because it ends the sessions; the third answers as in rekord-api).
- [ ] #10 Given a portal session opened at 10:00 on 2027-01-04 in Europe/Amsterdam, the time zone of every clock time in this criterion, for a CONFIRMED wedding dated 2027-12-31, and no further call of that session When the planner sets the wedding to CANCELLED at 11:00 on 2027-01-04 and back to CONFIRMED at 12:00 on 2027-01-19, and the session calls getPortalIdentity at 12:30 on 2027-01-19 Then the answer is 401 NOT_SIGNED_IN "Sign in to continue." because its 14-day idle end at 10:00 on 2027-01-18 has passed, and a new openPortalSession with that wedding's access code answers 200 (UD-18.e: a session works again only within its own 14-day and 30-day limits).
- [ ] #11 Given a COUPLE session on a live portal whose session row has revoked_at set directly in the test database, as closePortalSession sets it When its cookie is sent to getPortalIdentity Then the answer is 401 NOT_SIGNED_IN "Sign in to continue." (UD-10.a).
- [ ] #12 Given a CANCELLED wedding A whose COUPLE session and FRIENDS session were opened on 2027-06-01, before the planner set it to CANCELLED, and a CONFIRMED wedding B When the COUPLE session calls getPortalState with wedding B's COUPLE token and the FRIENDS session calls getPortalTasks with wedding A's FRIENDS token Then the answers are 401 BAD_LINK and 410 LINK_EXPIRED "This link retired after the wedding.", because the path-token check comes before the 410 check and the 410 check comes before the couple-only check (deviation UD-10.b; rekord-api answers 401 BAD_LINK and 403 FORBIDDEN).
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
