---
id: TASK-16.1
title: P2-E01-T01 Open a portal session with the access code (openPortalSession)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
  - stop-crypto-logging
milestone: m-2
dependencies: []
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:52-111'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:156-166'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalAttempts.java:28-61'
  - 'rekord-api/src/main/java/app/rekord/wedding/AccessCode.java:29-105'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:149-167'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:139-144'
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:188-202'
  - 'rekord-api/src/main/resources/db/migration/V2__sessions.sql:15-56'
  - 'docs/rewrite/analysis/12-couple-portal.md:74-80'
  - 'docs/rewrite/analysis/12-couple-portal.md:513'
  - 'docs/rewrite/analysis/12-couple-portal.md:655-689'
  - rekord-api/src/test/java/app/rekord/portal/PortalTest.java
  - 'rekord-api/src/main/resources/db/migration/V5__portals.sql:49-53'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-16
priority: high
type: feature
ordinal: 20101
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to open our portal with the access code on our card so that we reach our music wishes without an account.

The code gate of the couple portal, an authentication STOP item. rekord-api is the oracle: PortalGate.open and check (PortalGate.java:52-111) with the miss counting of PortalAttempts and the canonical matching of AccessCode; PortalTest has 7 cases, so this operation is not built test-first. Three deliberate deviations: the link has no stored expiry and its end is computed from the current wedding date at every check (UD-9.a); a CANCELLED wedding answers 410 LINK_EXPIRED to the right code (UD-10.b), and the same code opens the portal again once the wedding is no longer CANCELLED (UD-18.e); a portal without an access code refuses every code instead of accepting the literal PENDING that rekord-api stores (WeddingService.java:157-158, slice 12 R1, BR-CP-31). The gate checks in rekord-api's order (PortalGate.java:76-109): lock (429 CODE_LOCKED), revoked (right code 410 LINK_REVOKED, wrong code 401 BAD_LINK, no miss counted), past the computed end or CANCELLED (right code 410 LINK_EXPIRED, wrong code 401 BAD_LINK, no miss counted), code (miss counted, or count reset), deleted wedding (401 BAD_LINK). UD-18.f keeps the per-portal lockout as the only limit, with no per-client or global throttle, because the 256-bit token is the real boundary; the lock and its escalation are pinned in P2-E01-T04. This ticket changes no schema: the couple_portals columns failed_attempts, locked_until and last_seen_at (oracle V5 portals schema, lines 49-53) already exist, because P1-E04-T01 keeps every column of couple_portals except expires_at, and the sessions table already has portal_id and wedding_id from P1-E01-T02. The names in the answer are couple_display_name until P2-E02-T02 adds the names the couple enters, which the COUPLE and the FRIENDS scope then both see (UD-18.b, UD-19.a).

- Builds: `openPortalSession`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: UD-9.a, BR-CP-02, BR-CP-03, BR-CP-05, BR-CP-31, BR-ID-36, PIN-11-0393, PIN-12-0194, PIN-12-0513, UD-18.e, UD-19.d3
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P2-E01-T01` (user-story,P2,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When a visitor calls openPortalSession on 2027-06-01 with the COUPLE token and EJ12062027, and once more with the FRIENDS token and EJ12062027 Then the first answer is 200 with scope COUPLE, the wedding_id, names "Emma & Julian", wedding_date 2027-06-12 and days_until 11 plus a Set-Cookie rm_portal, and the second is 200 with scope FRIENDS and its own rm_portal cookie.
- [ ] #2 Given the same wedding and three more dated 2027-06-12, of the partners Sara and Sem, Anna and Eva, and Søren and Ole, whose COUPLE portals store the access codes SS12062027, AE12062027 and SO12062027 When a visitor calls openPortalSession with the first wedding's COUPLE token and each of "je 12.06.2027", "ej12062027" and "J E 1 2 0 6 2 0 2 7", and with the COUPLE tokens of the other three and "ß 12.06.2027", "æ12062027" and "øs 12062027" in that order Then each of the 6 answers is 200, because matching takes NFKD, strips marks, folds ø to O, ß to SS and æ to AE, upper-cases, drops everything outside A-Z and 0-9 and accepts either initial order (AccessCode.java:30-41, :76-83).
- [ ] #3 Given the same wedding When a visitor calls openPortalSession with an unknown 43-character token and EJ12062027, and with the COUPLE token and XX00000000 Then both answers are 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you." with no cookie, only the second counts a miss on the COUPLE portal, and neither answer leaves the server sooner than 300 ms after the request reached the gate.
- [ ] #4 Given the same wedding When openPortalSession answers 200, 401 BAD_LINK for an unknown token, 410 LINK_REVOKED and 429 CODE_LOCKED Then each answer leaves the server at least 300 ms after the request reached the gate.
- [ ] #5 Given the COUPLE portal of the same wedding revoked through revokeWeddingPortal When a visitor calls openPortalSession with the old token and EJ12062027, and with the old token and XX00000000 Then the first answers 410 LINK_REVOKED "This link was replaced. Ask your DJ for the new one.", the second answers 401 BAD_LINK, and neither counts a miss.
- [ ] #6 Given the COUPLE portal of the same wedding replaced through rotateWeddingPortal When a visitor calls openPortalSession with the old token and EJ12062027, and with the old token and XX00000000 Then the first answers 410 LINK_REVOKED "This link was replaced. Ask your DJ for the new one.", the second answers 401 BAD_LINK, and neither counts a miss.
- [ ] #7 Given the same wedding deleted through deleteWedding without purge, which sets revoked_at on its live portals (WeddingService.java:234-239) When a visitor calls openPortalSession with the COUPLE token and EJ12062027, and with the COUPLE token and XX00000000 Then the first answers 410 LINK_REVOKED "This link was replaced. Ask your DJ for the new one.", the second answers 401 BAD_LINK, and neither counts a miss, because the revoked check answers before the deleted-wedding check is reached.
- [ ] #8 Given the same wedding, once at 00:00 on 2027-06-20 in Europe/Amsterdam and once on 2027-06-01 with the wedding set to CANCELLED When a visitor calls openPortalSession with EJ12062027 and with XX00000000, and in the CANCELLED case once more with EJ12062027 after the planner sets the status back to CONFIRMED Then EJ12062027 answers 410 LINK_EXPIRED "This link retired after the wedding." and XX00000000 answers 401 BAD_LINK without counting a miss, and the call after the status is back to CONFIRMED answers 200 (deviation UD-10.b and UD-18.e for the CANCELLED case; rekord-api answers 200 to a CANCELLED wedding before its stored expiry).
- [ ] #9 Given the same wedding When a visitor calls openPortalSession with EJ12062027 at 12:00 on 2027-06-15, at 23:59 on 2027-06-19, and at 12:00 on 2027-07-08 after the planner moved the wedding date to 2027-07-03 through updateWedding Then all 3 answers are 200, because the end is 00:00 on the 8th day after the current wedding date in the wedding time zone, Europe/Amsterdam when the wedding has none (deviation UD-9.a; rekord-api answers 410 LINK_EXPIRED from 00:00 on 2027-06-13 for the first two, and keeps that stored end after the date move).
- [ ] #10 Given the same wedding with deleted_at set directly in the test database while its COUPLE portal keeps revoked_at null (slice 12 P11; deleteWedding would also revoke the portal) When a visitor sends 4 wrong codes, EJ12062027 and one more wrong code to openPortalSession Then all 6 answers are 401 BAD_LINK, no cookie is set, and the portal is not locked afterwards because the right code reset the count to 0.
- [ ] #11 Given a wedding created through createWedding with one PARTNER person, and one created with the single-word couple name "Emma" and no partners, so their portals carry no access code When a visitor calls openPortalSession with each wedding's COUPLE token and the code "pending", and with "PENDING" Then all 4 answers are 401 BAD_LINK and each counts a miss toward the lockout (deviation slice 12 R1, slice 11 R-03, BR-CP-31, P7; rekord-api answers 200 with a cookie to "pending").
- [ ] #12 Given the same wedding When a visitor calls openPortalSession with the body {}, with a body holding only the token, and with the body text "not json" sent as application/json Then all 3 answers are 422 VALIDATION_FAILED with no cookie and no miss counted, the errors of the first hold exactly {field token, code REQUIRED} and {field code, code REQUIRED}, those of the second exactly {field code, code REQUIRED}, and those of the third exactly {field null, code INVALID_FORMAT} (slice 12 P2; deviation UD-19.d3 for the third, whose rekord-api answer P2-E05-T03 criterion 3 records).
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
