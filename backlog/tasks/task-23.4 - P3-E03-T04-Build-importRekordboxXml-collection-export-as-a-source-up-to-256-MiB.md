---
id: TASK-23.4
title: >-
  P3-E03-T04 Build importRekordboxXml: collection export as a source, up to 256
  MiB
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-23.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:150-163'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:206-222'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:57-95'
  - 'rekord-api/src/main/java/app/rekord/library/RekordboxImport.java:36-215'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:434-456'
  - 'rekord-backend/server/rekordbox_import.py:20-45'
  - 'rekord-contract/paths/dj.yaml:246-292'
  - 'rekord-contract/components/library.yaml:301-320'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:143-150'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:404'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:432'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:82-95'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-23
priority: high
type: feature
ordinal: 30304
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to import my rekordbox collection export so that the tempo and key rekordbox analysed come along with every track.

The body is the XML itself, read by a streaming parser with DTDs and external entities off (BR-LIB-28). Only COLLECTION tracks with a Location count; each location becomes a native path (BR-LIB-29) and the fields map as rekord-api maps them (BR-LIB-30). Locations are percent-decoded the way rekord-backend decodes them: a + stays a +, and a % that starts no escape stays as it is; rekord-api's form decoding turns + into a space and fails the file on a stray % (analysis 13 D12). Because %2B and a literal + both decode to +, the stored path equals the real file path whichever way rekordbox escapes it (PIN-13-0490). The upload limit is 256 MiB answered in the error envelope, and the body is read as a stream so that an oversized upload is never held in memory whole; rekord-api's HTTP layer refuses everything over 10 MiB first with a 413 that has no envelope (RISK-41). The XML export is one source of the library named after the upload (BR-LIB-11), and its rekordbox tempo and key survive a later folder rescan (BR-LIB-14). missing_files counts named paths that do not exist on the machine running wedding-portal (BR-LIB-32); that check stays (UD-19.m2). library_id is a query parameter of this operation (rekord-contract/paths/dj.yaml:271-276), and UD-19.i2 covers request-body ids only, so a library_id that no library has, or one of another DJ or of another business, keeps rekord-api's 404 NO_LIBRARY, as the query parameter library_id of importPlaylist does (P3-E06-T02).

- Builds: `importRekordboxXml`
- Covers: BR-LIB-27, BR-LIB-28, BR-LIB-29, BR-LIB-30, BR-LIB-31, BR-LIB-32, BR-LIB-55, BR-LIB-11, BR-LIB-14, PIN-13-0496, PIN-13-0507, PIN-13-0489, PIN-13-0490, RISK-41, PIN-AC-0512, UD-19.m2
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the unique-constraint race of this operation is settled by P3-E09-T02 (both 200, never 500)

Plan item `P3-E03-T04` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library and an export whose COLLECTION holds TRACK elements with the Locations "file://localhost/C:/M%C3%BAsica/T%C3%A9%20st.mp3" and "file://localhost/Users/dj/Music/a.mp3", and whose PLAYLISTS node holds a TRACK with only Key="1" When the DJ calls importRekordboxXml Then the answer is 200 with imported 2, and the stored paths are "C:\Música\Té st.mp3" and "/Users/dj/Music/a.mp3" (BR-LIB-29).
- [ ] #2 Given an export track with Name "Song", Mix "Extended Mix", Artist "Artist A", Album "Album A", TotalTime "245", BitRate "320", AverageBpm "400.00", Tonality "Am", no Size and a .aif location When the DJ imports it Then the track has title "Song (Extended Mix)", duration_sec 245, bitrate_kbps 320, bpm 400.0, musical_key "Am", ext "aiff", size_bytes 0, mtime_ms 0 and tag_source "rekordbox" (BR-LIB-30).
- [ ] #3 Given export tracks with Name "Song (Extended Mix)" and Mix "extended mix", with Name and Mix empty, and with AverageBpm "0" When the DJ imports them Then the titles are "Song (Extended Mix)" and the file's stem, and the third track's bpm is null (BR-LIB-30).
- [ ] #4 Given an export naming one audio path twice and the path C:/Music/notes.txt When the DJ imports it Then the audio path is imported once, and warnings holds "skipped non-audio entry: notes.txt", with every warning returned and no cap (BR-LIB-29, BR-LIB-32).
- [ ] #5 Given export Locations ending in "A+B.mp3", "C%2BD.mp3" and "100%.mp3" When the DJ imports them Then the stored file names are "A+B.mp3", "C+D.mp3" and "100%.mp3" (fixed defect PIN-13-0489, PIN-13-0490; rekord-api stores "A B.mp3" and answers 400 BAD_XML for the stray %).
- [ ] #6 Given a body that is not well-formed XML, a well-formed document with no DJ_PLAYLISTS root and no tracks, and a DJ_PLAYLISTS root with no tracks When the DJ imports each Then each answers 400 BAD_XML with the messages "That is not valid XML.", the Export Collection hint, word for word as RekordboxImport.java:146-148 writes it, and "The collection in this XML file is empty." (BR-LIB-31).
- [ ] #7 Given an export with a DOCTYPE declaring an external entity that names a file on the server, used in a TRACK Name When the DJ imports it Then the answer is 400 BAD_XML and the named file is never read (BR-LIB-28).
- [ ] #8 Given an active library When the DJ calls importRekordboxXml with an empty body, with an 11 MiB export and with a body of 256 MiB plus 1 byte Then the answers are 400 EMPTY_FILE "That file is empty.", 200, and 413 FILE_TOO_LARGE "That file is too large to import." in the error envelope (fixed defect RISK-41, PIN-13-0496; rekord-api answers a 413 with no envelope above 10 MiB).
- [ ] #9 Given a DJ with no active library, a library of another DJ of the same business, a library of another business and the library_id 999999 that no library has When the DJ imports a valid export without library_id and then with each of those three library_ids Then the answers are 400 NO_LIBRARY_SELECTED with the message "Select a library first." and three times 404 NO_LIBRARY with the message "That library does not exist.", no source is written, and an empty or oversized body is refused before either library check (LibraryService.java:70-78; LibraryRepository.java:89-95).
- [ ] #10 Given an export imported with name " Gig.xml " and one imported without name When the DJ reads the library's sources Then they are the xml sources labelled "Gig.xml" and "rekordbox.xml", and a second import named "Gig.xml" reuses its source and replaces its tracks, so a track the new file no longer names loses that source's claim (BR-LIB-11).
- [ ] #11 Given a scanned file with no BPM tag and key tag "5A", then an import of the same path with AverageBpm "124" and Tonality "Am", then a forced rescan of the folder When the DJ reads the track Then bpm is 124.0 and musical_key "Am", while title, artist and the other descriptive columns hold the latest writer's values (BR-LIB-14).
- [ ] #12 Given an export naming 3 paths of which 1 exists on the disk of the machine running wedding-portal, imported with the library_id of the DJ's library B while A is active When the DJ reads the answer Then imported is 3, missing_files is 2, and library describes B: active_library_id and active_library_name name B and the counts and sources are B's, while the DJ's selection stays A (fixed defect BR-LIB-55, PIN-13-0507; rekord-api returns A's summary).
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
