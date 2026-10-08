---
id: TASK-25.1
title: >-
  P3-E05-T01 Build listPreferences: the active library's remembered choices with
  honest file labels
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.7
  - TASK-24.1
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/PreferenceService.java:45-81'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:186-226'
  - 'rekord-contract/components/library.yaml:565-611'
  - 'rekord-contract/paths/dj.yaml:501-515'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:306-311'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:423'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:511'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:598'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-25
priority: high
type: feature
ordinal: 30501
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see every version choice I remembered in the library I work in so that I can review and undo them before a gig.

A remembered choice says: when this song comes up again, use this file. It belongs to one library and is keyed by the signature id of the stripped artist and title (P3-E04-T01), not by track id, so it survives the library changing around it. The list is ordered by artist, then title, and with no active library it is empty rather than an error (PreferenceService.java:45-81). rekord-api joins file_label from the global tracks table, so the label stays set while any library of any business still holds the path, which hides the DJ app's warning that the file is no longer in this library and leaks across businesses (RISK-29, BR-MX-21). wedding-portal fills file_label only when the active library still holds the track through a claim of one of its sources, as the contract states (library.yaml:585-588).

- Builds: `listPreferences`
- Covers: BR-MX-21, RISK-29, PIN-14-0423, PIN-14-0511
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E05-T01` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ whose active library L holds the file random_name.mp3 and remembered choices for "Beatles" / "Yesterday", "Abba" / "Waterloo" and "Abba" / "Fernando" When the DJ calls listPreferences Then the answer is 200 with preferences ordered Abba / Fernando, Abba / Waterloo, Beatles / Yesterday, and each item carries id (16 hex characters), artist, title, track_id, chosen_at with a UTC offset and file_label (MatchingResource.java:211-226).
- [ ] #2 Given a remembered choice whose track is a file of L named random_name with extension mp3 When the DJ calls listPreferences Then its file_label is "random_name.mp3" (MatchingResourceTest.java:147-181).
- [ ] #3 Given a DJ with no active library When the DJ calls listPreferences Then the answer is 200 with preferences [] and no error (PreferenceService.java:45-50).
- [ ] #4 Given a DJ who remembered a file in library A, and library B of another business that holds the same absolute path through its own source When the DJ removes that source from A and calls listPreferences with A active Then the choice is still listed with its track_id and its file_label is null (fixed defect RISK-29, PIN-14-0423, PIN-14-0511; rekord-api answers the file name from the global tracks table).
- [ ] #5 Given a remembered choice whose track lost its last claim, so its tracks row is gone (LibraryRepository.java:306-311) When the DJ calls listPreferences Then the choice is still listed with file_label null (BR-MX-21).
- [ ] #6 Given a remembered choice of the DJ's library M that is not the active one, and a remembered choice of another DJ's library When the DJ calls listPreferences with L active Then neither appears in the answer.
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
