---
id: TASK-10.4
title: P1-E04-T04 Edit a wedding with the COMPLETED rule of UD-11 (updateWedding)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:112-124'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:169-215'
  - >-
    rekord-api/src/main/resources/db/migration/V11__relax_assignment_constraints.sql:25-33
  - 'docs/rewrite/STATUS.md:261-278'
  - 'rekord-contract/components/planner.yaml:473-536'
  - 'rekord-contract/paths/planner.yaml:42-110'
  - 'docs/rewrite/analysis/11-planner-weddings.md:94-96'
  - 'docs/rewrite/analysis/11-planner-weddings.md:398-401'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-10
priority: high
type: feature
ordinal: 10404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to edit a wedding's details and status while a wedding can only be COMPLETED with a venue, a DJ and a photographer settled so that a finished wedding never lacks the facts the team worked from.

Builds the operation updateWedding. Oracle: WeddingsResource.updateWedding (WeddingsResource.java:112-124) and WeddingService.update (WeddingService.java:169-215): only the non-null fields of the body are written, so nothing can be cleared; couple name, date, guest count, venue vendor, venue name, status and note are taken, while timezone, partners, venue note and the three times are ignored (BR-PL-20). A changed date marks the live portals' code stale and keeps the code (BR-PL-14, BR-CP-12); a new couple name changes neither the partners, the slug nor the code (RISK-31, BR-PL-09, PIN-15-0449). The body is WeddingInput, so couple_display_name and wedding_date are required. Deviation UD-11: a wedding may be set to, and stay, COMPLETED only while it has a venue (a venue vendor or a venue name) and its DJ and PHOTO slots are each PENCILLED, CONFIRMED or NOT_NEEDED; the rule is evaluated on every update of a wedding that is or becomes COMPLETED, every violation is collected and the refusal is one 422 VALIDATION_FAILED whose message names each and whose errors list has one item per violation with the field venue, team.DJ or team.PHOTO (UD-11.a, UD-11.b, UD-12). Every other status move is free (UD-11.c, BR-PL-29). rekord-api answers 500 for a COMPLETED wedding without venue (BR-PL-22) and accepts OPEN slots. A negative guest_count answers 422 instead of 500 (UD-12, BR-PL-23). Setting a COMPLETED wedding's DJ or PHOTO slot to OPEN is refused in P1-E05-T06.

- Builds: `updateWedding`
- Covers: BR-PL-20, BR-PL-14, BR-PL-22, BR-PL-23, BR-PL-29, BR-CP-12, BR-DM-17, RISK-31, PIN-15-0078, PIN-15-0449, UD-11.a, UD-11.c
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E04-T04` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027, note "vegan menu" and guest_count 80 When a planner calls updateWedding with couple_display_name "Emma & Julian", wedding_date 2027-06-12, guest_count 120, timezone Europe/London and note null Then the answer is 200 with guest_count 120, note "vegan menu", timezone Europe/Amsterdam and the other fields unchanged (BR-PL-20).
- [ ] #2 Given the same wedding with its FRIENDS portal revoked, and a second wedding dated 2027-06-12 with live portals, access code EJ12062027 and code_stale false When a planner calls updateWedding on the first with wedding_date 2027-07-03, and on the second with wedding_date 2027-06-12 Then the live COUPLE portal of the first keeps code EJ12062027 with code_stale true, its revoked FRIENDS row is unchanged, and the portals of the second keep code_stale false (BR-PL-14, BR-CP-12).
- [ ] #3 Given the same wedding When a planner calls updateWedding with couple_display_name "Emma & Jules" Then the display name is "Emma & Jules", the PARTNER people are still Emma and Julian, the slug stays emma-julian, and the code stays EJ12062027 with code_stale false (RISK-31, PIN-15-0449).
- [ ] #4 Given a wedding with venue name "De Oude Tuinderij", its DJ slot CONFIRMED and its PHOTO slot OPEN When a planner calls updateWedding with status COMPLETED Then the answer is 422 VALIDATION_FAILED naming the PHOTO slot with one errors item {field team.PHOTO, code INVALID_VALUE}, and the status is unchanged (deviation UD-11.a).
- [ ] #5 Given a wedding without a venue whose DJ slot is PENCILLED and PHOTO slot is NOT_NEEDED When a planner calls updateWedding with status COMPLETED Then the answer is 422 VALIDATION_FAILED naming the venue with one errors item {field venue, code INVALID_VALUE}, and the status is unchanged (deviation UD-11.b; rekord-api answers 500, BR-PL-22, PIN-15-0078).
- [ ] #6 Given a wedding without a venue whose DJ and PHOTO slots are OPEN When a planner calls updateWedding with status COMPLETED Then the answer is 422 VALIDATION_FAILED whose message names the venue, the DJ slot and the PHOTO slot, with the errors items venue, team.DJ and team.PHOTO in that order, each with code INVALID_VALUE (UD-12, UD-19.d1).
- [ ] #7 Given a wedding without a venue whose DJ slot is PENCILLED and PHOTO slot is NOT_NEEDED When a planner calls updateWedding with status COMPLETED and venue_name "De Oude Tuinderij" Then the answer is 200 with status COMPLETED (UD-11.a).
- [ ] #8 Given a COMPLETED wedding When a planner calls updateWedding with guest_count 90, then with status CANCELLED, then with status DRAFT, then with status CONFIRMED Then every answer is 200 and the last status stored is CONFIRMED (UD-11.c, BR-PL-29).
- [ ] #9 Given a wedding When a planner calls updateWedding without wedding_date, and with guest_count -1 Then both answers are 422 VALIDATION_FAILED, the first with the errors item {field wedding_date, code REQUIRED} and the second with {field guest_count, code INVALID_VALUE}, and the wedding is unchanged (deviation UD-12 for guest_count; rekord-api answers 500, BR-PL-23).
- [ ] #10 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls updateWedding with each Then each answer is 404 NO_WEDDING "There is no such wedding.", and a DJ named on the wedding's CONFIRMED DJ slot gets 403 with the wedding unchanged.
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
