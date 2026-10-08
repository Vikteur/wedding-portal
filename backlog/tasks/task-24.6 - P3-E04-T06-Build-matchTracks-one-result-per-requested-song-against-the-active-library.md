---
id: TASK-24.6
title: >-
  P3-E04-T06 Build matchTracks: one result per requested song against the active
  library
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
  - stop-contract-push
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-24.3
  - TASK-24.5
  - TASK-2
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:70-153'
  - 'rekord-contract/paths/dj.yaml:1-7'
  - 'rekord-contract/paths/dj.yaml:470-499'
  - 'rekord-contract/components/library.yaml:404-421'
  - 'rekord-contract/components/library.yaml:448-563'
  - 'rekord-api/src/test/java/app/rekord/matcher/MatchingResourceTest.java:79-301'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:260-268'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:438'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:509'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:596-597'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: feature
ordinal: 30406
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to send a list of requested songs and get each one back with its candidates and a verdict so that I build the night's set from my own files.

The operation POST /api/match over the matcher of this epic. It reads the active library, refuses while that library is scanning, takes the index of the library or of one of its imported playlists from the cache, and answers one result per requested track in request order with library_size and library_name (BR-LIB-46). The contract makes index and title required and tracks at least 1 long, so validation answers 422 before the resource runs and the 400 NO_TRACKS of rekord-api is unreachable (BR-MX-19, BR-MX-20, BR-MX-46). rekord-api does not check playlist_id: an unknown id, or one of another library, narrows to nothing and answers 409 NO_LIBRARY "library is empty", which misleads the DJ; wedding-portal answers 422 VALIDATION_FAILED naming playlist_id for an id that does not exist or belongs to another business (deviation UD-19.i2), and 404 NO_PLAYLIST for a playlist of the business that is not one of the active library's (fixed defect, analysis 13 R6, PIN-13-0509, one part of RISK-49), following the contract's rule that something outside the caller's scope answers 404. The contract adds maxItems: 1000 to the tracks of MatchRequest (rekord-contract components/library.yaml:533-547), so a request of 1001 tracks answers 422 before the resource runs, and CI asserts no time limit on the call (deviation UD-19.m5, PIN-14-0517, RISK-49). The contract change lands in rekord-contract before the code (contract-first) as a pull request labelled semver:major, because it narrows a request the contract accepted (UD-19.e), and pushing it is a STOP item that needs the user's approval at that time.

- Builds: `matchTracks`
- STOP (human approval in the pull request): contract-push, auth-access
- Covers: BR-MX-19, BR-MX-20, BR-MX-46, BR-LIB-43, BR-LIB-46, PIN-14-0506, PIN-13-0509, RISK-49, PIN-14-0517, UD-19.m5, UD-19.i2
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: besides the 409 NO_LIBRARY of an empty index, asserted by the fourth criterion, requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `NO_TRACKS` without a criterion here: unreachable: validation answers 422 VALIDATION_FAILED for an empty tracks list before the resource runs (BR-MX-20, BR-MX-46); asserted by the third criterion
- Error code `UNKNOWN` without a criterion here: the catch-all 500 UNKNOWN of P0-E05 for an unexpected failure; matchTracks writes no rows, so no constraint race applies
- Error code `VALIDATION_FAILED` without a criterion here: a request body or parameter that breaks the contract's constraints answers 422 VALIDATION_FAILED through P0-E05 (UD-12)

Plan item `P3-E04-T06` (user-story,P3,stop-contract-push,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ whose active library holds one file tagged "Daft Punk" / "One More Time" at 322 seconds When the DJ calls matchTracks with index 0, that artist and title and duration_sec 320 Then the answer is 200 with library_size 1, library_name the library's name, and one result: input echoes the track, input_version is descriptors [] and remixer null, bucket is auto, auto_selected_id is the file's id, from_preference is false and the one candidate carries track (the stored file's id, path, filename, ext, artist, title, album, duration_sec 322, bitrate_kbps, bpm, musical_key, tag_source, size_bytes, mtime_ms), score 1.0, parts, version, duration_delta_sec 2.0 and playlists [] (BR-LIB-46).
- [ ] #2 Given an active library holding one file tagged "Daft Punk" / "One More Time" and one file tagged "Cher" / "Believe" When the DJ calls matchTracks with the indexes 7, 3 and 5 for "Daft Punk" / "One More Time", for artist " Ghost Act " and title "A Song Nobody Owns!", and for "Cher" / "Believe" Then the results come back in that order with input.index 7, 3 and 5, and the second result has bucket unmatched, candidates [], auto_selected_id null, input.artist " Ghost Act " and input.title "A Song Nobody Owns!" byte for byte, so the DJ sees the song as entered (UD-19.c, which keeps rekord-api's echo of the input; MatchingResource.java:92-100, :168-175; MatchingResourceTest.java:116-146).
- [ ] #3 Given a DJ When the DJ calls matchTracks with tracks [], with a track without index and with a track without title Then each answer is 422 VALIDATION_FAILED in the error envelope, and none is 400 NO_TRACKS (BR-MX-19, BR-MX-20, BR-MX-46, PIN-14-0506).
- [ ] #4 Given a DJ with no active library, a DJ whose active library has a scan in state scanning and a DJ whose active library holds no files When each calls matchTracks with one valid track Then the answers are 400 NO_LIBRARY_SELECTED with the message "Select a library first.", 409 SCAN_IN_PROGRESS with the message "Wait for the scan to finish." and 409 NO_LIBRARY with the message "The selected library is empty: scan a folder or import a rekordbox XML." (BR-LIB-46; MatchingResource.java:72-90, LibraryRepository.java:215-222).
- [ ] #5 Given an active library holding f1, f2 and f3 with the files f1 and f3 tagged with the same song and an imported playlist P of that library holding only f3 When the DJ calls matchTracks for that song with playlist_id P Then library_size is 1 and the only candidate is f3 (BR-LIB-43; MatchingResourceTest.java:283-301).
- [ ] #6 Given the playlist_id 999999 that no playlist has, a playlist of a library of another business, a playlist of another DJ's library in the same business and a playlist of the DJ's own library that is not the active one When the DJ calls matchTracks with each Then the first two answer 422 VALIDATION_FAILED with the errors item field "playlist_id" and code "INVALID_VALUE" (deviation UD-19.i2; rekord-api answers 409 NO_LIBRARY), and the last two answer 404 NO_PLAYLIST with the message "That playlist is gone." (fixed defect PIN-13-0509, analysis 13 R6; rekord-api answers 409 NO_LIBRARY or matches the overlap silently).
- [ ] #7 Given a DJ who matched a song against the active library L, then scanned a folder that adds a file of that song and waited for state done When the DJ calls matchTracks for the song again Then the new file is among the candidates (fixed defect RISK-25; rekord-api keeps the index built before the scan ended).
- [ ] #8 Given an active library of 20,000 generated files When the DJ calls matchTracks with 100 tracks, with 1000 tracks and with 1001 tracks Then the first two answer 200 with one result per track, the third answers 422 VALIDATION_FAILED with the errors item field "tracks" and code "TOO_LONG", and no test of the build asserts a time limit on any of the three calls (deviation UD-19.m5; rekord-api answers 200 to 1001 tracks; PIN-14-0517, RISK-49).
- [ ] #9 Given a candidate whose tracks row is deleted after the index was built and before the answer is mapped When the DJ calls matchTracks Then that candidate is left out of the answer and the others are returned (MatchingResource.java:117-153).
- [ ] #10 Given an active library with no files When the DJ calls matchTracks with the playlist_id 999999 and then with a playlist of the DJ's own library that is not the active one Then the answers are 422 VALIDATION_FAILED naming playlist_id and 404 NO_PLAYLIST, not 409 NO_LIBRARY, because the playlist check runs after 400 NO_LIBRARY_SELECTED and 409 SCAN_IN_PROGRESS and before 409 NO_LIBRARY (MatchingResource.java:72-90).
- [ ] #11 Given rekord-contract/components/library.yaml with MatchRequest.tracks holding minItems 1 (library.yaml:533-547) When the contract change of this ticket is read Then MatchRequest.tracks holds minItems 1 and maxItems 1000, and no other line of the contract changes (deviation UD-19.m5, PIN-14-0517).
- [ ] #12 Given the contract pull request of this ticket, labelled semver:major because a maximum on an existing request list refuses a request the contract accepted before (UD-19.e) When the contract CI of P0-E02 runs and the pull request is merged after its contract-push approval Then lint passes, the breaking-change job passes whether or not it lists the new maxItems as an ERR-level change, the pull request raises info.version by the major part and the merge commit gets the tag v<info.version> (UD-20.b), and the build of wedding-portal that ships this ticket pins that tag (UD-19.e, UD-19.m5).
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
