---
id: TASK-26.2
title: >-
  P3-E06-T02 Build importPlaylist: resolve a playlist upload against a library
  and keep it by name
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-26.1
  - TASK-24.6
  - TASK-25.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:31-228'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:165-179'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:206-220'
  - 'rekord-contract/paths/dj.yaml:309-344'
  - 'rekord-contract/components/library.yaml:322-381'
  - 'rekord-api/src/test/java/app/rekord/library/LibraryExportTest.java:186-244'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:159-166'
  - 'rekord-api/src/main/java/app/rekord/matcher/Matcher.java:92-124'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30602
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to import a playlist export into my library so that the files I already play rank first and I can narrow matching to that playlist.

The operation POST /api/library/playlists takes the raw body, the optional query name (the upload's file name) and the optional library_id; without library_id it uses the active library. The checks run in this order: a body over 8 MiB, the library, the reader of P3-E06-T01, an empty library, then resolution (LibraryService.java:103-138). An entry with a path resolves exactly when the track id derived from that path is in the library's index; any other entry with an artist or a title goes through the matcher of P3-E04 without a duration and resolves to the first candidate only when its score is 0.75 or more; a track is kept once, in first-seen order (BR-LIB-37). Re-importing a name that matches an existing playlist of the library, without letter case, replaces its tracks and missing count (BR-LIB-39). The answer gives resolved and missing counts and at most 12 missing examples (BR-LIB-40), and the library's playlists. Every import invalidates the index cache of the library (P3-E04-T05). The matcher call takes the library's remembered choices (P3-E05-T02) and its playlist membership, so a remembered file is tried first, and a row still resolves only when its own score is 0.75 or more (LibraryService.java:147-180).

- Builds: `importPlaylist`
- Covers: BR-LIB-33, BR-LIB-37, BR-LIB-38, BR-LIB-39, BR-LIB-40
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the unique-constraint race of this operation is settled by P3-E09-T02 (both 200, never 500)

Plan item `P3-E06-T02` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ whose active library L holds the scanned files f1 and f2 When the DJ calls importPlaylist with an M3U naming the absolute paths of f1 and f2 and the name "Most played.m3u8" Then the answer is 200 with playlist_id, name "Most played", resolved 2, missing 0, missing_examples [] and playlists holding that playlist with library_id L, added_at, track_count 2 and missing_count 0 (LibraryExportTest.java:186-200).
- [ ] #2 Given L holding only "Daft Punk" / "One More Time" When the DJ imports a TXT export with the rows "One More Time (Kygo Remix)" by "Daft Punk", "One More Time" by "Other Band" and "Believe" by "Cher" Then resolved is 1 (the first row scores 0.8676), missing is 2 (the second row's best score is 0.7214, below 0.75, and the third has no candidate) and missing_examples is ["Other Band - One More Time", "Cher - Believe"] (BR-LIB-37, BR-LIB-40).
- [ ] #3 Given an active library L holding a file a tagged "Daft Punk" / "One More Time" and a file b tagged "Daft Punk" / "One More Time (Kygo Remix)", and a remembered choice of b for "Daft Punk" / "One More Time" When the DJ imports a TXT export with the one row "One More Time" by "Daft Punk" Then resolved is 1 and getImportedPlaylistTracks lists b, not a, because the remembered file is tried first and scores 0.8676, at or above 0.75 (BR-LIB-37; LibraryService.java:153-171, Matcher.java:92-124).
- [ ] #4 Given an M3U naming the path of f1 three times and then the path of f2 When the DJ imports it Then resolved is 2 and getImportedPlaylistTracks lists f1 then f2; and given 15 entries that resolve to nothing plus one that resolves, the answer has missing 15 and exactly the first 12 missing entries as missing_examples, each "Artist - Title" or the title alone when the artist is empty (LibraryService.java:128-137).
- [ ] #5 Given L with an imported playlist "Most played" of 1 track and 1 missing entry When the DJ imports a file named "MOST PLAYED.m3u8" whose 2 entries resolve Then listImportedPlaylists lists one playlist for that name, still called "Most played", with track_count 2 and missing_count 0 (BR-LIB-39; LibraryExportTest.java:220-234).
- [ ] #6 Given a body of 8 MiB plus 1 byte When the DJ calls importPlaylist Then the answer is 413 FILE_TOO_LARGE with the message "That playlist file is too large." in the error envelope, before the library is looked up, so a DJ with no active library also gets 413 FILE_TOO_LARGE and not 400 NO_LIBRARY_SELECTED; and given a DJ whose active library L holds files and a body of 0 bytes the answer is 400 EMPTY_FILE with the message "That file is empty." (BR-LIB-33; LibraryService.java:103-110, PlaylistImport.java:92-93).
- [ ] #7 Given a DJ with no active library and no library_id, a library_id of another DJ's library and an unknown library_id When the DJ calls importPlaylist with a valid M3U Then the answers are 400 NO_LIBRARY_SELECTED with the message "Select a library first.", 404 NO_LIBRARY and 404 NO_LIBRARY, and no playlist row is written; and with library_id of the DJ's own library M, not the active one, the playlist is written to M.
- [ ] #8 Given an active library with no files When the DJ imports a valid M3U Then the answer is 400 NO_TRACKS with the message "Scan a folder into this library before importing a playlist into it."; and given L with files and an M3U whose only entry is "C:\Somewhere\Else\nothing.mp3" the answer is 400 NOTHING_RESOLVED with the message "None of those tracks are in this library."; no playlist row is written in either case (BR-LIB-38; LibraryExportTest.java:236-244).
- [ ] #9 Given an upload with no entries When the DJ calls importPlaylist Then the answer is 400 BAD_PLAYLIST with the message "No tracks found in that file. Export the playlist from rekordbox (right-click the playlist, then Export) as m3u8, txt, pls or xml." from the reader of P3-E06-T01.
- [ ] #10 Given a DJ who called matchTracks with playlist_id P for a song held by f1 only When the DJ re-imports P with a file that names f2 instead, a file of the same song, and calls matchTracks with playlist_id P again Then the only candidate is f2, because the import invalidated the cached index of the library (P3-E04-T05; LibraryService.java:126).
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
