---
id: TASK-11.1
title: >-
  P1-E05-T01 List a wedding's people and add a person (listWeddingPeople,
  addWeddingPerson)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - test-first
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:134-156'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:270-280'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:244-248'
  - rekord-api/src/main/java/app/rekord/domain/WeddingPerson.java
  - 'rekord-contract/components/planner.yaml:44-120'
  - 'rekord-contract/components/planner.yaml:784-792'
  - 'rekord-contract/paths/planner.yaml:140-182'
  - 'rekord-api/src/main/resources/db/migration/V4__weddings.sql:61-87'
  - 'docs/rewrite/analysis/11-planner-weddings.md:114'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10501
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to keep the partners, family and MC of a wedding as separate people with their contact details so that the team knows who is who on the day.

Builds the operations listWeddingPeople and addWeddingPerson. Oracle: WeddingsResource.listWeddingPeople and addWeddingPerson (WeddingsResource.java:134-156, :270-280) with WeddingService.peopleOf (WeddingService.java:244-248). The list is ordered by kind as text (FAMILY, MC, OTHER, PARTNER), then by sort order (BR-PL-30). addWeddingPerson stores kind, given name, family name, note, phone, e-mail and sort order from the body, a missing sort order becoming 0, and answers 201 with the whole PersonList of the wedding, not the new person. Adding a PARTNER changes neither the portal code nor code_stale, and "exactly two partners" is not enforced: a third partner with a free sort order is stored. The wedding is found with the visibility rule of BR-PL-03, so an assigned DJ lists the people and anyone who does not see the wedding gets 404 NO_WEDDING "There is no such wedding.". Deviation: a PARTNER whose sort order another PARTNER of the wedding already has is refused with 422 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" and the errors item {field sort_order, code INVALID_VALUE} instead of the 500 of ux_wedding_people_partner_slot (UD-12, BR-PL-30, BR-DM-18); two calls that race past that check clash on ux_wedding_people_partner_slot and get the same refusal, answered with 409 (deviation UD-19.i5, P0-E05-T05). New message text (no oracle): "sort_order is already used by another partner of this wedding". rekord-api has no test for either operation, so the ticket is test-first (UD-15.d).

- Builds: `listWeddingPeople`, `addWeddingPerson`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-30, BR-DM-18, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E05-T01` (user-story,P1,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for listWeddingPeople When a characterization test calls listWeddingPeople against rekord-api on a wedding with people of each kind, and for an unknown wedding id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given rekord-api as the behaviour oracle and no rekord-api test for addWeddingPerson When a characterization test calls addWeddingPerson against rekord-api with a FAMILY person, with a third PARTNER at sort order 2, with a PARTNER at a taken sort order and without given_name Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #3 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When a planner calls addWeddingPerson with kind FAMILY, given_name "Ingrid", family_name "Smit", note "mother of the bride", phone "+31 6 1234 5678" and email "ingrid@example.com" and no sort_order Then the answer is 201 with people Ingrid (FAMILY, sort_order 0, every given field as sent), Emma (PARTNER, 0) and Julian (PARTNER, 1) in that order (BR-PL-30).
- [ ] #4 Given that wedding When a planner calls addWeddingPerson with kind MC and given_name "Ray" and sort_order 3, then with kind OTHER and given_name "Lotte" Then listWeddingPeople answers 200 with Ingrid, Ray, Lotte, Emma, Julian in that order.
- [ ] #5 Given that wedding When a planner calls addWeddingPerson with kind PARTNER, given_name "Sam" and sort_order 2 Then the answer is 201 with three PARTNER people, and both portals keep code EJ12062027 with code_stale false.
- [ ] #6 Given that wedding When a planner calls addWeddingPerson with kind PARTNER, given_name "Sam" and sort_order 1, and with kind PARTNER and no sort_order Then both answers are 422 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" with the errors item {field sort_order, code INVALID_VALUE}, and the people are unchanged (deviation UD-12; rekord-api answers 500, BR-DM-18).
- [ ] #7 Given that wedding When a planner calls addWeddingPerson without kind, with given_name "", and with kind "BRIDE" Then each answer is 422 VALIDATION_FAILED, with the errors item {field kind, code REQUIRED}, {field given_name, code TOO_SHORT} and {field kind, code INVALID_VALUE} in that order of calls, and the people are unchanged (UD-19.d1, UD-19.d3).
- [ ] #8 Given that wedding with a DJ member named on its DJ slot as PENCILLED, and a second DJ member named on no slot When each DJ calls listWeddingPeople Then the first answer is 200 with the people and the second is 404 NO_WEDDING "There is no such wedding." (BR-PL-03).
- [ ] #9 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls listWeddingPeople and addWeddingPerson with each Then each answer is 404 NO_WEDDING "There is no such wedding." and no person is stored, and a member holding only DJ calling addWeddingPerson on a wedding they are named on gets 403.
- [ ] #10 Given that wedding and a test latch in the person repository adapter that holds both calls after the sort-order check When two planners call addWeddingPerson at the same time with kind PARTNER and sort_order 2, one with given_name "Sam" and one with given_name "Noor" Then one answers 201 and the other answers 409 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" with the errors item {field sort_order, code INVALID_VALUE}, and the wedding holds one PARTNER at sort_order 2 (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
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
