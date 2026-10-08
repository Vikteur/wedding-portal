---
id: TASK-12.2
title: >-
  P1-E06-T02 Show a wedding's portal links with the computed end date
  (getWeddingPortal)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-contract-push
  - stop-crypto-logging
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:224-229'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:255-258'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:265-270'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:133-157'
  - 'rekord-api/src/main/java/app/rekord/wedding/PortalService.java:139-144'
  - 'rekord-contract/components/planner.yaml:541-590'
  - 'rekord-contract/paths/planner.yaml:282-301'
  - 'docs/rewrite/STATUS.md:241-251'
  - 'docs/rewrite/STATUS.md:257-259'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: feature
ordinal: 10602
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to read the couple's and the friends' link with their codes and the day they stop working so that I can give them to the couple again when they lose them.

Builds the operation getWeddingPortal and the PortalLink aggregate. Oracle: WeddingsResource.getWeddingPortal (WeddingsResource.java:224-229), WeddingService.portalsOf (WeddingService.java:265-270) and WeddingMapper.portalLinks and portalLink (WeddingMapper.java:133-157). Only live portals (revoked_at null) are shown: the COUPLE portal fills couple, the FRIENDS portal fills friends, and a scope without a live portal is left empty. Each PortalLink carries scope, url (the public base address plus /g/ plus the token), code, code_stale, revoked, expires_at and last_seen_at; a portal that stores no code shows code PENDING, as decided in P1-E04-T02 (RISK-02). PortalLink is its own aggregate holding the wedding by id, and the Wedding aggregate holds no portal collection (UD-10.d). Deviation: expires_at is not stored (P1-E04-T01 creates couple_portals without it) but computed on every read as 00:00 on the 8th day after the wedding's current date in the wedding's time zone, written as an instant in UTC, so it follows a date move (UD-9.a, UD-10.c, RISK-20; rekord-api stores the start of the day after the wedding at create and at rotate only, BR-PL-13, PIN-15-0083). The rule is the function added by P1-E04-T02, held by the PortalLink aggregate, which the portal gate of P2-E01 calls for its own check. The same computed expires_at appears in every PortalLink wedding-portal returns, including the portal field of createWedding and getWedding (P1-E04-T02). Only the description of expires_at in the contract changes (planner.yaml:568-572), so the ticket carries the STOP item contract-push. The operation needs the PLANNER role; the wedding is found with WeddingsResource.require (WeddingsResource.java:255-258).

- Builds: `getWeddingPortal`
- STOP (human approval in the pull request): contract-push, crypto-logging, auth-access
- Covers: UD-10.c, UD-10.d, RISK-20, BR-PL-13, PIN-15-0083
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E06-T02` (user-story,P1,stop-contract-push,stop-crypto-logging,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When a planner calls getWeddingPortal Then the answer is 200 with couple scope COUPLE and friends scope FRIENDS, each with a url of the public base address plus /g/ plus its 43-character token, code EJ12062027, code_stale false, revoked false, expires_at 2027-06-19T22:00:00Z (00:00 on 2027-06-20 in Europe/Amsterdam) and last_seen_at null (deviation UD-9.a, UD-10.c; rekord-api answers 2027-06-12T22:00:00Z).
- [ ] #2 Given that wedding When a planner calls updateWedding with wedding_date 2027-07-03 and then getWeddingPortal Then both links carry expires_at 2027-07-10T22:00:00Z (00:00 on 2027-07-11 in Europe/Amsterdam) and the database holds no expiry column on couple_portals (deviation RISK-20; rekord-api keeps the old expiry, PIN-15-0083).
- [ ] #3 Given a wedding dated 2027-06-12 in time zone America/New_York with live portals When a planner calls getWeddingPortal Then both links carry expires_at 2027-06-20T04:00:00Z.
- [ ] #4 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 with its FRIENDS portal revoked, and a second wedding with both portals revoked When a planner calls getWeddingPortal for each Then the first answer has the couple link and no friends link, and the second answer has no couple link and no friends link.
- [ ] #5 Given a wedding whose portals store no code and code_stale true When a planner calls getWeddingPortal Then both links show code PENDING and code_stale true (RISK-02, P1-E04-T02).
- [ ] #6 Given the contract file rekord-contract/components/planner.yaml When the change of this ticket is reviewed Then the only change to it is the description of PortalLink.expires_at, which reads "Links stop working at 00:00 on the eighth day after the wedding date, in the wedding's time zone.", and the push waits for the human approval of the STOP item contract-push (UD-10.c).
- [ ] #7 Given the domain model When a unit test builds a PortalLink for a wedding id, a scope, a token and a code Then no Wedding instance is needed, the PortalLink holds the wedding only by id, and the Wedding type has no field that holds PortalLink values (UD-10.d).
- [ ] #8 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls getWeddingPortal with each Then each answer is 404 NO_WEDDING "There is no such wedding.", and a member holding only DJ named on the wedding's DJ slot gets 403.
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
