---
id: TASK-11.2
title: >-
  P1-E05-T02 Edit a person, marking the code stale when a partner changes
  (updatePerson)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-11.1
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:158-174'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:261-280'
  - 'rekord-contract/components/planner.yaml:59-120'
  - 'rekord-contract/paths/planner.yaml:184-226'
  - 'docs/rewrite/analysis/11-planner-weddings.md:114'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10502
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to fix a person's details while a partner's change flags the couple's code so that I decide myself whether to issue a new code.

Builds the operation updatePerson. Oracle: WeddingsResource.updatePerson (WeddingsResource.java:158-174, :261-280): every field of the body replaces the stored one, so a missing family name, note, phone or e-mail clears it, while a missing sort order keeps the stored one (BR-PL-30). When the person is a PARTNER after the change, every live portal of the wedding gets code_stale true and keeps its code; a revoked portal is not touched, and a person who stops being a PARTNER marks nothing (BR-PL-14). The answer is the Person. The person is found by id and then through its wedding's visibility rule: an unknown person is 404 NO_WEDDING "There is no such person.", a person on a wedding the caller does not see is 404 NO_WEDDING "There is no such wedding." (BR-PL-04). Deviation: a PARTNER sort order already used by another PARTNER of the wedding is refused with 422 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" and the errors item {field sort_order, code INVALID_VALUE} instead of the 500 of ux_wedding_people_partner_slot (UD-12, BR-PL-30); two calls that race past that check clash on ux_wedding_people_partner_slot and get the same refusal, answered with 409 (deviation UD-19.i5, P0-E05-T05). New message text (no oracle): "sort_order is already used by another partner of this wedding". rekord-api has no test for this operation, so the ticket is test-first (UD-15.d).

- Builds: `updatePerson`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-PL-30, BR-PL-14, BR-PL-04, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E05-T02` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for updatePerson When a characterization test calls updatePerson against rekord-api on a PARTNER, on a FAMILY person, with a missing sort_order, with a taken PARTNER sort order and for an unknown person id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and the FAMILY person Ingrid with family_name "Smit", phone "+31 6 1234 5678" and sort_order 2 When a planner calls updatePerson with kind FAMILY and given_name "Ingrid" only Then the answer is 200 with family_name, note, phone and email null and sort_order 2, and both portals keep code_stale false (BR-PL-30).
- [ ] #3 Given that wedding When a planner calls updatePerson on the PARTNER Julian with kind PARTNER and given_name "Jules" Then the answer is 200 with given_name "Jules", and both live portals keep code EJ12062027 with code_stale true (BR-PL-14).
- [ ] #4 Given that wedding with its FRIENDS portal revoked When a planner calls updatePerson on the PARTNER Emma with kind PARTNER and given_name "Emma" Then the COUPLE portal gets code_stale true and the revoked FRIENDS row keeps code_stale false.
- [ ] #5 Given that wedding When a planner calls updatePerson on Ingrid with kind PARTNER, given_name "Ingrid" and sort_order 2, and then on Ingrid with kind FAMILY Then the first answer marks both live portals code_stale true, and after resetting code_stale to false in the test the second marks nothing.
- [ ] #6 Given that wedding When a planner calls updatePerson on Ingrid with kind PARTNER and sort_order 0 Then the answer is 422 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" with the errors item {field sort_order, code INVALID_VALUE}, and Ingrid is unchanged (deviation UD-12; rekord-api answers 500, BR-DM-18).
- [ ] #7 Given that wedding When a planner calls updatePerson on Ingrid without kind, and with given_name "" Then both answers are 422 VALIDATION_FAILED, the first with the errors item {field kind, code REQUIRED} and the second with {field given_name, code TOO_SHORT}, and Ingrid is unchanged (UD-19.d1).
- [ ] #8 Given an unknown person id When a planner calls updatePerson with it Then the answer is 404 NO_WEDDING "There is no such person." (BR-PL-04).
- [ ] #9 Given a person on a wedding of another business and a person on a wedding with deleted_at set When a planner calls updatePerson on each Then each answer is 404 NO_WEDDING "There is no such wedding." and the person is unchanged, and a member holding only DJ named on the wedding's DJ slot gets 403.
- [ ] #10 Given that wedding with the FAMILY person Ingrid and the OTHER person Lotte, and a test latch in the person repository adapter that holds both calls after the sort-order check When two planners call updatePerson at the same time, one on Ingrid and one on Lotte, each with kind PARTNER and sort_order 2 Then one answers 200 and the other answers 409 VALIDATION_FAILED "sort_order is already used by another partner of this wedding" with the errors item {field sort_order, code INVALID_VALUE}, and the wedding holds one PARTNER at sort_order 2 (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
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
