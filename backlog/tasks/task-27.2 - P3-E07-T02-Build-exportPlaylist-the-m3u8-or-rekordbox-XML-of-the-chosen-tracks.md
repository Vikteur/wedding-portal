---
id: TASK-27.2
title: >-
  P3-E07-T02 Build exportPlaylist: the m3u8 or rekordbox XML of the chosen
  tracks
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-27.1
  - TASK-23
  - TASK-5
references:
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:61-102'
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:154-240'
  - 'rekord-api/src/test/java/app/rekord/library/LibraryExportTest.java:76-115'
  - 'rekord-api/src/test/java/app/rekord/library/LibraryExportTest.java:245-253'
  - 'rekord-contract/components/library.yaml:615-639'
  - 'rekord-contract/paths/dj.yaml:578-615'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:374'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:427'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-27
priority: high
type: feature
ordinal: 30702
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to download the tracks I picked as an m3u8 or a rekordbox XML file named after the playlist so that I import the set into rekordbox in the order I chose.

The operation POST /api/export takes name, format (m3u8 or xml), track_ids (at least one) and the optional wedding_id (library.yaml:615-639). It uses the active library, reads each requested track, keeps request order and drops a repeated id, and answers the body of P3-E07-T01 as a download (ExportsResource.java:61-102). The download name is built as rekord-api builds it, accents kept (BR-MX-34, UD-16.UX-15), not as the Python POC built it (UX-15). One change from rekord-api: the body is written straight into the response instead of a temporary file kept on disk for up to an hour (analysis 14:427); BR-MX-36, which describes that temporary file, is settled by removing it. rekord-api reads each requested track id with no library scope (RISK-09, analysis 13:374); wedding-portal refuses every request position whose track id the active library does not hold with 422 VALIDATION_FAILED, one errors item per such position with field track_ids[i], i the 0-based position, and code INVALID_VALUE, and writes no file (deviation UD-19.m7). An unknown format value answers 422 with the errors item field format and code INVALID_VALUE (UD-19.d3). The never list behind wedding_id is P3-E07-T05.

- Builds: `exportPlaylist`
- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-24, BR-MX-31, BR-MX-34, BR-MX-36, PIN-14-0504, PIN-14-0507, PIN-14-0263, PIN-14-0505, RISK-09, UD-19.m7
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `NO_TRACKS` without a criterion here: answered 400 when every requested track is on the never list of the given wedding; specified by P3-E07-T05, because an empty track_ids answers 422 VALIDATION_FAILED first
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the export writes no rows, so no race of it reaches this code
- Error code `UNKNOWN_TRACK` without a criterion here: not answered by exportPlaylist of wedding-portal: a track id the active library does not hold answers 422 VALIDATION_FAILED naming track_ids[i] (deviation UD-19.m7; rekord-api answers 400 UNKNOWN_TRACK), asserted by the eighth and ninth criteria

Plan item `P3-E07-T02` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and an active library holding two scanned files When a characterization test calls exportPlaylist with no Accept header, once with format m3u8 and once with format xml, and records the status, the Content-Type (media type and charset) and the Content-Disposition of each Then the same test passes against wedding-portal, so each Content-Type is the one rekord-api sends for that format and a media type the pinned contract declares for the 200 response, audio/x-mpegurl or application/xml (PIN-14-0263, PIN-14-0505; paths/dj.yaml:605-613).
- [ ] #2 Given a DJ whose active library holds the scanned files f1 and f2 When the DJ calls exportPlaylist with name "Set", format m3u8 and track_ids [f2, f1, f2] Then the answer is 200 with the m3u8 body of P3-E07-T01 listing f2 once, then f1 (LibraryExportTest.java:76-89, ExportsResource.java:79-89).
- [ ] #3 Given the same library When the DJ calls exportPlaylist with name "Set", format xml and track_ids [f1] Then the answer is 200 with the rekordbox XML of P3-E07-T01 whose playlist node is named "Set", and the collection import of P3-E03 reads it back to exactly one track with the path of f1 (LibraryExportTest.java:104-115).
- [ ] #4 Given the name "Emma & Julián" When the DJ calls exportPlaylist with format m3u8 and then with format xml Then the Content-Disposition headers are attachment; filename="Emma & Juli_n.m3u8"; filename*=UTF-8''Emma%20%26%20Juli%C3%A1n.m3u8 and the same with the extension .xml: the plain name turns every non-ASCII character and every double quote into _, and the encoded name keeps A-Z, a-z, 0-9 and -._~ and percent-encodes every other UTF-8 byte in upper case (BR-MX-34, UD-16.UX-15; LibraryExportTest.java:91-102, ExportsResource.java:173-208).
- [ ] #5 Given the names "AC/DC: Live?" and three spaces When the DJ calls exportPlaylist with format m3u8 Then the download names are "AC DC  Live.m3u8" and "playlist.m3u8": the name is stripped, an empty name becomes "playlist", each of \ / : * ? " < > | and each CR or LF becomes a space, and the result is stripped again (BR-MX-34; ExportsResource.java:154-162).
- [ ] #6 Given a DJ with an active library When the DJ calls exportPlaylist with track_ids [], then with format "wav" and an otherwise valid body, then without name Then each answer is 422 VALIDATION_FAILED from the contract's constraints, the one for format "wav" with the errors item field "format" and code "INVALID_VALUE" (UD-19.d3), so the NO_TRACKS refusal of an empty list at ExportsResource.java:66 is never reached (BR-MX-24, BR-MX-31, PIN-14-0504, PIN-14-0507).
- [ ] #7 Given a DJ with no active library When the DJ calls exportPlaylist with a valid body Then the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
- [ ] #8 Given an active library holding f1 When the DJ calls exportPlaylist with track_ids [f1, "deadbeef0000"] Then the answer is 422 VALIDATION_FAILED with one errors item, field "track_ids[1]" and code "INVALID_VALUE", and no file, even though f1 exists (deviation UD-19.m7; rekord-api answers 400 UNKNOWN_TRACK with the message "One of those tracks is no longer in the library."; LibraryExportTest.java:245-253, ExportsResource.java:81-85).
- [ ] #9 Given a DJ whose active library L1 does not hold the track t, which the DJ's other library L2 holds When the DJ calls exportPlaylist with track_ids [t] Then the answer is 422 VALIDATION_FAILED with one errors item, field "track_ids[0]" and code "INVALID_VALUE", and no file (deviation UD-19.m7, RISK-09; rekord-api answers 200 with the path of t, ExportsResource.java:68-84, LibraryRepository.java:352-359).
- [ ] #10 Given a DJ who exports a playlist When wedding-portal answers exportPlaylist Then the body is written straight into the response and no file is created under the temporary directory of the JVM (fixed defect, analysis 14-dj-matching-exports.md:427: export temp files hold paths and names for up to 1 h; rekord-api writes each body to java.io.tmpdir/rekord-exports and sweeps it after 1 h, ExportsResource.java:49-50, :173-185, :210-240).
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
