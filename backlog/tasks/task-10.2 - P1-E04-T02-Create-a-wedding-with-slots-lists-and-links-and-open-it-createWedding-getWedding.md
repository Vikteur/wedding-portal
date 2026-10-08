---
id: TASK-10.2
title: >-
  P1-E04-T02 Create a wedding with slots, lists and links, and open it
  (createWedding, getWedding)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-10.1
  - TASK-9.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:87-110'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:255-258'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:282-288'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:49-167'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:276-311'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:51-131'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:145-157'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:176-210'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:86-94'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:126-133'
  - 'rekord-api/src/main/java/app/rekord/wedding/AccessCode.java:29-66'
  - 'rekord-contract/components/planner.yaml:343-536'
  - 'rekord-contract/paths/planner.yaml:34-110'
  - 'docs/rewrite/analysis/11-planner-weddings.md:81-108'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-10
priority: high
type: feature
ordinal: 10402
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want one call to create a wedding with its partners, team slots, song lists and both portal links so that I can hand the couple their link the moment the wedding exists.

Builds the operations createWedding and getWedding. Oracle: WeddingsResource.createWedding and getWedding (WeddingsResource.java:87-110) with WeddingService.create (WeddingService.java:49-167) and WeddingMapper.detail (WeddingMapper.java:72-131, :145-157, :176-210). One transaction stores the wedding, up to two PARTNER people, one WAITING song list per kind, one OPEN slot for VENUE, DJ, CATERING, PHOTO and MC, and a live COUPLE and FRIENDS portal with their own 43-character token and the same code (BR-PL-07, BR-CP-08). Partners are the given names of the first two partners items, all their other fields dropped, or else the couple name split on &, +, and or en between spaces into exactly two parts, first word of each (BR-PL-10). The code is the canonical first letters of the two given names plus the date as ddMMyyyy (BR-PL-11, BR-PL-12). The slug is the slugified couple name, wedding when empty, with -2, -3 appended while a live wedding of the business has it (BR-PL-09). Status defaults to DRAFT, timezone to Europe/Amsterdam (BR-PL-08, BR-PL-29); note, venue_note and the three times of the body are not stored, as in the oracle. The link url is app.public-base-url followed by /g/ and the token (BR-PL-19). This ticket adds the expiry function of a link: expires_at is computed on every read as 00:00 on the 8th day after the wedding's current date in the wedding's time zone, written as an instant in UTC, and never stored (UD-9.a, UD-10.c); P1-E06-T02 places it in the PortalLink aggregate. days_until counts calendar days from today in the wedding's own time zone (WeddingService.java:276-283, BR-PL-25). getWedding returns the same detail to a caller who sees the wedding: a planner sees every live wedding of the business, anyone else only a wedding with a PENCILLED or CONFIRMED slot naming their account; not visible is 404 NO_WEDDING "There is no such wedding." (BR-PL-03, BR-PL-04). Deviations: a portal whose code cannot be derived stores no code and code_stale true, and its PortalLink shows the text PENDING as rekord-api shows it (RISK-02, BR-DM-23; the placeholder PENDING is kept by the user's decision UD-19.h); COMPLETED at create is refused by UD-11, because every slot starts OPEN; a negative guest_count and an unknown timezone answer 422 VALIDATION_FAILED instead of 500 (UD-12, BR-PL-08, BR-PL-23). The portal of getWedding for an assigned DJ is RISK-04 of phase 3. Admin visibility is P1-E09-T02.

- Builds: `createWedding`, `getWedding`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-PL-03, BR-PL-04, BR-PL-05, BR-PL-06, BR-PL-07, BR-PL-08, BR-PL-09, BR-PL-10, BR-PL-11, BR-PL-12, BR-PL-19, BR-PL-23, BR-PL-25, BR-PL-28, BR-PL-29, BR-DM-16, BR-DM-23, BR-DM-25, BR-CP-08, BR-ID-22, RISK-02, PIN-15-0077, UD-19.h
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN stays the answer when two creates with the same couple name race past the free-slug check and the second fails on ux_weddings_slug (BR-PL-09, P0-E05-T05); the slug check picks a free slug instead of refusing the request, so no domain refusal exists for the 409 of UD-19.i5 (P1-E09-T07); no other rule of this operation raises it.

Plan item `P1-E04-T02` (user-story,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the application clock fixed at 2027-06-01T10:00Z and a planner When the planner calls createWedding with couple_display_name "Emma & Julian", wedding_date 2027-06-12 and partners Emma and Julian Then the answer is 201 with slug emma-julian, status DRAFT, timezone Europe/Amsterdam, days_until 11, venue null, people Emma (sort_order 0) and Julian (sort_order 1) of kind PARTNER, team CATERING, DJ, MC, PHOTO and VENUE all OPEN, and music lists_in 0 of lists_total 5 (BR-PL-07, BR-PL-25; deviation UD-18.c; rekord-api answers lists_total 6).
- [ ] #2 Given that answer and app.public-base-url unset, so it defaults to http://localhost:8080 (application.properties:35) When the test reads song_lists and portal Then song_lists holds the 7 kinds by order with their seeded labels and max_songs, state WAITING and song_count 0, portal is the COUPLE link with url http://localhost:8080/g/ plus a 43-character token, code EJ12062027, code_stale false, revoked false and expires_at 2027-06-19T22:00:00Z (deviation UD-10.c; rekord-api answers 2027-06-12T22:00:00Z), and the database holds a live FRIENDS portal with code EJ12062027 and another token (BR-PL-19, BR-CP-08, BR-DM-25).
- [ ] #3 Given a planner When the planner calls createWedding with couple_display_name "Emma van Dijk en Julian Smit" and no partners, and with partners Émile (family_name "Laurent") and Øystein Then the first wedding has the PARTNER people Emma and Julian and code EJ12062027, and the second has Émile with family_name null and Øystein and code EO12062027 (BR-PL-10, BR-PL-12).
- [ ] #4 Given a planner When the planner calls createWedding with couple_display_name "Emma, Julian" and no partners, with only the partner Emma, and with the partners Emma and "!!" Then each answer is 201, and each wedding's portals store no code, have code_stale true and show code PENDING (deviation RISK-02; rekord-api stores the text PENDING, BR-DM-23, BR-PL-11).
- [ ] #5 Given a planner When the planner calls createWedding twice with "Emma & Julian", once with "!!!", then sets deleted_at of the first directly in the test and calls createWedding with "Emma & Julian" again Then the slugs are emma-julian, emma-julian-2, wedding and emma-julian (BR-PL-09, BR-DM-16, PIN-15-0077).
- [ ] #6 Given the application clock fixed at 2027-06-01T10:00Z When a planner calls createWedding with status CONFIRMED, timezone Pacific/Kiritimati, wedding_date 2027-06-01, venue_name "De Oude Tuinderij", venue_note "rain plan", ceremony_time "14:00" and note "vegan menu" Then the answer has status CONFIRMED, timezone Pacific/Kiritimati, days_until -1, venue with vendor_id null, name "De Oude Tuinderij" and note null, ceremony_time null and note null (BR-PL-25, BR-PL-28, BR-PL-29).
- [ ] #7 Given a vendor of the business When a planner calls createWedding with its id as venue_vendor_id and no venue_name Then the answer's venue has that vendor_id and name "" (BR-PL-28).
- [ ] #8 Given a planner When the planner calls createWedding with status COMPLETED and no venue, and once with status COMPLETED and venue_name "De Oude Tuinderij" Then the first answer is 422 VALIDATION_FAILED with the errors items {field venue, code INVALID_VALUE}, {field team.DJ, code INVALID_VALUE} and {field team.PHOTO, code INVALID_VALUE}, the second is 422 VALIDATION_FAILED with the items {field team.DJ, code INVALID_VALUE} and {field team.PHOTO, code INVALID_VALUE}, and no wedding is stored (deviation UD-11.b, UD-19.d1; rekord-api answers 500 for the first and 201 for the second).
- [ ] #9 Given a planner When the planner calls createWedding without couple_display_name, without wedding_date, with 3 partners, with guest_count -1, and with timezone "Mars/Olympus" Then every answer is 422 VALIDATION_FAILED with one errors item, in that order of calls {field couple_display_name, code REQUIRED}, {field wedding_date, code REQUIRED}, {field partners, code TOO_LONG}, {field guest_count, code INVALID_VALUE} and {field timezone, code INVALID_VALUE}, and no wedding, person, slot, list or portal is stored (deviation UD-12 for guest_count and timezone, UD-19.d1; rekord-api answers 500, BR-PL-08, BR-PL-23).
- [ ] #10 Given a wedding created by a planner When the planner calls getWedding with its id Then the answer is 200 with the same detail createWedding returned, and no field of the answer holds a song title or the DJ briefing (BR-PL-05, BR-PL-06).
- [ ] #11 Given that wedding with a DJ slot naming a DJ member's account as PENCILLED, and a second wedding naming that account on an OPEN slot When the DJ calls getWedding on each Then the first answer is 200 and the second is 404 NO_WEDDING "There is no such wedding." (BR-PL-03, BR-ID-22).
- [ ] #12 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls getWedding with each Then each answer is 404 NO_WEDDING "There is no such wedding." (BR-PL-04).
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
