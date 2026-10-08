---
id: TASK-26.4
title: >-
  P3-E06-T04 Build getImportedPlaylistTracks test-first: a playlist's files in
  import order
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
  - test-first
milestone: m-3
dependencies:
  - TASK-26.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:190-196'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:260-281'
  - 'rekord-contract/paths/dj.yaml:368-389'
  - 'rekord-contract/components/library.yaml:383-392'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:167'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:440'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30604
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see which files an imported playlist holds so that I can check what the import resolved before I narrow matching to it.

Returns the library tracks of one imported playlist in import order (BR-LIB-41). The playlist is reached through its library, so the library's visibility rule applies: an unknown playlist id answers 404 NO_PLAYLIST, and a playlist of a library the caller cannot see answers 404 NO_LIBRARY (LibraryService.java:260-268). It needs no active library, so a playlist of the DJ's own library that is not the active one is readable. rekord-api has no test for this operation, so a characterization test runs first (UD-15.d).

- Builds: `getImportedPlaylistTracks`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E06-T04` (user-story,P3,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and a DJ whose library L holds a playlist P imported with f2 then f1 When a characterization test calls getImportedPlaylistTracks for P, for an unknown playlist id and for a playlist of another DJ's library Then it records 200 with tracks [f2, f1], 404 NO_PLAYLIST with the message "That playlist is gone." and 404 NO_LIBRARY with the message "That library does not exist.", and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a playlist P of L imported with f3, f1 and f2 When the DJ calls getImportedPlaylistTracks for P Then the answer is 200 with tracks f3, f1, f2 in that order, each a LibraryTrack whose id, path, filename, ext, artist, title, album, duration_sec, bitrate_kbps, bpm, musical_key, tag_source, size_bytes and mtime_ms equal the stored file (BR-LIB-41; LibraryService.java:270-281).
- [ ] #3 Given a playlist of the DJ's library M, which is not the active one When the DJ calls getImportedPlaylistTracks for it Then the answer is 200 with its tracks.
- [ ] #4 Given a playlist P of L imported with f1, f2 and f3, and a rescan of L that no longer finds f2, which no other library holds When the DJ calls getImportedPlaylistTracks for P Then the tracks are f1 and f3, in that order (RISK-36; LibraryService.java:270-281).
- [ ] #5 Given a playlist P of L imported with f1, f2 and f3, a rescan of L that no longer finds f2, and a later rescan that finds f2 at the same path again When the DJ calls getImportedPlaylistTracks for P Then the tracks are f1, f2 and f3, because the playlist rows have no foreign key to the files and are kept (P3-E02-T01).
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
