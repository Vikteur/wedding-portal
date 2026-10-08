---
id: TASK-25.2
title: >-
  P3-E05-T02 Build rememberPreference and let a remembered choice win in
  matching
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-25.1
  - TASK-24.6
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/PreferenceService.java:83-114'
  - 'rekord-api/src/main/java/app/rekord/matcher/Matcher.java:92-124'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:192-197'
  - 'rekord-contract/components/library.yaml:590-601'
  - 'rekord-contract/paths/dj.yaml:516-539'
  - >-
    rekord-api/src/test/java/app/rekord/matcher/MatchingResourceTest.java:147-253
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:138'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:142'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:429'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:509'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-25
priority: high
type: feature
ordinal: 30502
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to tell the matcher which file to use for a song so that every later playlist that asks for that song gets my chosen version.

rememberPreference strips artist and title, stores an absent artist as "", refuses an empty title and refuses a track that is not in the active library's full index, unnarrowed by any playlist (BR-MX-22); a track_id that no library holds, or that only libraries of another business hold, answers 422 VALIDATION_FAILED naming track_id (deviation UD-19.i2). A new signature inserts; an existing one updates only track_id and chosen_at, so the first wording of artist and title is kept. The answer is the full list of P3-E05-T01. The matcher of P3-E04 gains its last step (BR-MX-18, Matcher.java:92-124): the choice for the query's signature moves its file to the front of the candidates, or scores and inserts it first when scoring did not list it but the index used for this match holds it, re-capping the list at 8; auto_selected_id becomes that file and from_preference true, and the bucket is left as scoring set it. The signature condition of UD-19.c (P3-E04-T03) is evaluated on scoring's highest-scoring candidate, before the remembered choice is applied; the remembered choice then only sets auto_selected_id and from_preference, so it preselects its file in an ambiguous result too and never turns an ambiguous or unmatched result into auto, and a remembered file that does not carry the query's signature leaves an auto bucket auto (UD-19.c: a remembered choice of this DJ still preselects). A choice whose file is not in that index is ignored. Choices are read from the database on every match (P3-E04-T05). rekord-api reads then inserts with no lock, so two first calls for one song make one of them 500 (PIN-14-0429); wedding-portal writes the row as an upsert on (library_id, id).

- Builds: `rememberPreference`
- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-18, BR-MX-22, PIN-14-0429, PIN-14-0509, UD-19.i2
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the primary-key race of two first calls for one song answers 200 to both, asserted by the sixth criterion
- Error code `VALIDATION_FAILED` without a criterion here: a request body or parameter that breaks the contract's constraints answers 422 VALIDATION_FAILED through P0-E05 (UD-12)

Plan item `P3-E05-T02` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ whose active library L holds file f When the DJ calls rememberPreference with artist "  Daft Punk ", title " One More Time " and track_id f Then the answer is 200 with one preference whose id is "4cad791cad0c0451", artist "Daft Punk", title "One More Time", track_id f and file_label the file name of f.
- [ ] #2 Given a remembered choice "Daft Punk" / "One More Time" for file f1 of L When the DJ calls rememberPreference with artist "daft punk", title "one more time" and file f2 of L Then the list still holds one preference with id "4cad791cad0c0451", track_id f2, a later chosen_at, and artist "Daft Punk" and title "One More Time" unchanged (BR-MX-22; MatchingResourceTest.java:200-215).
- [ ] #3 Given an active library L with file f When the DJ calls rememberPreference with no artist, title "Anthem" and track_id f Then the stored preference has artist "" and id "7298f481ab00c4b9".
- [ ] #4 Given an active library L When the DJ calls rememberPreference with title "   ", with the track_id "deadbeef0000" that no library holds, with the id of a file that only a library of another business holds, and with the id of a file that only another library of the DJ holds Then the answers are 400 EMPTY_TITLE with the message "A remembered choice needs a song title.", twice 422 VALIDATION_FAILED with the errors item field "track_id" and code "INVALID_VALUE" (deviation UD-19.i2; rekord-api answers 400 UNKNOWN_TRACK to both), and 400 UNKNOWN_TRACK with the message "That track is not in this library." (PreferenceService.java:83-95).
- [ ] #5 Given a DJ When the DJ calls rememberPreference with a body without title, and with a body without track_id Then each answer is 422 VALIDATION_FAILED (PreferenceInput requires title and track_id); and given a DJ with no active library and a full body the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
- [ ] #6 Given an active library L with no choice for "Daft Punk" / "One More Time" When two rememberPreference calls for that song and file f of L run in parallel Then both answer 200 and L holds exactly one preference row for that id (fixed defect PIN-14-0429, PIN-14-0509; rekord-api answers 500 UNKNOWN to one of them).
- [ ] #7 Given L holding a "Daft Punk" / "One More Time" at 320 seconds and b "Daft Punk" / "One More Time (Kygo Remix)" at 320 seconds, and a remembered choice of b for "Daft Punk" / "One More Time" When the DJ calls matchTracks for "Daft Punk" / "One More Time" at 320 seconds Then the candidates are b with score 0.8875 then a with score 1.0, auto_selected_id is b, from_preference is true and the bucket is auto, the bucket the same call answers without the choice, because scoring's leader a carries the query's signature (BR-MX-18, UD-19.c; MatchingResourceTest.java:147-181).
- [ ] #8 Given L holding a "Daft Punk" / "One More Time" at 320 seconds and c "Cher" / "Believe" at 240 seconds, and a remembered choice of c for "Daft Punk" / "One More Time" When the DJ calls matchTracks for "Daft Punk" / "One More Time" at 320 seconds Then the candidates are c with score 0.27 then a with score 1.0, auto_selected_id is c, from_preference is true and the bucket is auto, because the signature condition of UD-19.c reads scoring's leader a, which carries the query's signature, and the remembered choice c does not change the bucket (UD-19.c, BR-MX-18).
- [ ] #9 Given a song whose scoring lists 8 candidates and a remembered choice for it of a ninth file of L that scoring did not list When the DJ calls matchTracks for the song Then the answer lists 8 candidates, the remembered file first, followed by the first 7 that scoring listed (Matcher.java:98-108).
- [ ] #10 Given a remembered choice of file b for a song, and then the source of b removed from L When the DJ calls matchTracks for the song Then the choice is ignored: auto_selected_id is the file scoring picked and from_preference is false.
- [ ] #11 Given a remembered choice of file b of L for a song and an imported playlist P of L that does not hold b When the DJ calls matchTracks for the song with playlist_id P Then the choice is ignored and from_preference is false, because the narrowed index does not hold b (BR-MX-18).
- [ ] #12 Given L holding only a "Daft Punk" / "One More Time" at 320 seconds and a remembered choice of a for artist "" and title "One More Time" When the DJ calls matchTracks with artist "", title "One More Time" and duration_sec 320 Then the only candidate is a with score 1.0, the bucket is ambiguous, auto_selected_id is a and from_preference is true, because a query without an artist is never auto and the remembered choice only preselects (deviation UD-19.c; rekord-api answers auto with a and from_preference true).
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
