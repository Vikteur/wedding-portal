---
id: TASK-27.5
title: >-
  P3-E07-T05 Apply the couple's never list to the playlist and missing-list
  exports
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-27.2
  - TASK-27.3
  - TASK-24
  - TASK-19
  - TASK-21.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/Blocklist.java:14-68'
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:75-93'
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:111-125'
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:150-152'
  - 'rekord-api/src/test/java/app/rekord/library/LibraryExportTest.java:117-184'
  - 'rekord-api/src/main/java/app/rekord/scanner/FilenameParse.java:26-40'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:375'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:419-425'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:43-54'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-27
priority: high
type: feature
ordinal: 30705
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want the exports for a wedding to leave out every version of the songs on the couple's never list so that a banned song cannot reach the decks even when I picked it by hand.

When exportPlaylist or exportMissing carries a wedding_id, every entry of that wedding's song lists whose kind is a blocklist becomes a key of normalised artist and normalised core title, the version stripped by the matcher rules of P3-E04 (Blocklist.java:37-60). A track or missing entry is dropped silently when its key matches, or when an entry without artist matches its core title (BR-MX-25, BR-LIB-47, BR-CP-30). An export left empty is refused with 400 NO_TRACKS (BR-LIB-48). One fix: an item whose normalised artist is empty, either a library file without an artist tag or a missing entry without an artist, is caught by a never-list entry with the same core title that names an artist (fixed defect RISK-28). Never-list keys are built from each entry's artist and title only; free_text is never read, and a typed-in entry is keyed on the title it was copied into (Blocklist.java:44-60; SongService.java:47-50). A wedding_id is checked against the visibility of getDjWedding (P3-E01-T02): the id of a wedding whose slots the caller does not fill in state PENCILLED or CONFIRMED, of a wedding of another business, of a soft-deleted wedding or of no wedding answers 404 NO_WEDDING with the message "There is no such wedding." and no file, while an admin sees every wedding of the business (deviation UD-19.m7, RISK-09; UD-14.b3). For wedding_id, UD-19.m7 takes precedence over UD-19.i2: a wedding of another business and an unknown id answer 404 NO_WEDDING, never 422 VALIDATION_FAILED naming wedding_id. The check runs where rekord-api loads the never list: after the active-library check and, for exportPlaylist, after the track-id check, and before the never list is applied. Unless a criterion says otherwise, the DJ fills the DJ slot of every wedding named below in state CONFIRMED.

- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-25, BR-MX-26, BR-LIB-47, BR-LIB-48, BR-CP-30, RISK-28, PIN-14-0419, PIN-14-0510, PIN-13-0510, PIN-14-0516, PIN-14-0425, UD-19.m7

Plan item `P3-E07-T05` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding W whose never list holds ("Purple Disco Machine", "Substitution") and an active library holding "Substitution (Extended Mix)" by Purple Disco Machine and "Am I Wrong" When the DJ calls exportPlaylist with both tracks and wedding_id W Then the answer is 200 and the file lists "Am I Wrong" only (BR-MX-25, BR-LIB-47; LibraryExportTest.java:117-136).
- [ ] #2 Given a never-list entry with title "Dreams" and no artist When the DJ exports "Dreams" by Fleetwood Mac and "Dreams (Extended Mix)" by another artist with that wedding_id Then both are left out, because an entry without artist blocks the title from every artist (Blocklist.java:62-68).
- [ ] #3 Given the same tracks When the DJ calls exportPlaylist without wedding_id and with the id of a wedding whose never list is empty, and a member holding only the ADMIN role who fills no slot of W calls exportPlaylist with the two tracks of the first criterion and wedding_id W Then both of the DJ's answers are 200 with every track, nothing filtered, and the admin's answer is 200 with the file listing "Am I Wrong" only (BR-LIB-48; deviation UD-14.b3; rekord-api answers the role-denied 403 to an admin; ExportsResource.java:150-152).
- [ ] #4 Given a wedding whose must_plays list, a song-list kind that is not a blocklist, holds "Am I Wrong" When the DJ exports that track with the wedding_id Then the track is kept, because only kinds flagged is_blocklist count (BR-MX-26; Blocklist.java:44-60).
- [ ] #5 Given W and an export of only "Substitution (Extended Mix)" When the DJ calls exportPlaylist with wedding_id W Then the answer is 400 NO_TRACKS with the message "Every track in that export is on the couple's never list." and no file (BR-LIB-48; LibraryExportTest.java:138-149).
- [ ] #6 Given W and track_ids holding the blocked track and "deadbeef0000" When the DJ calls exportPlaylist with wedding_id W Then the answer is 422 VALIDATION_FAILED with one errors item, field "track_ids[1]" and code "INVALID_VALUE", because every id is checked before the never list empties the export (deviation UD-19.m7; rekord-api answers 400 UNKNOWN_TRACK; ExportsResource.java:79-93).
- [ ] #7 Given W and a library file Substitution.mp3 with no tags, which the scan reads as no artist and the title "Substitution" (FilenameParse.java:26-40) When the DJ calls exportPlaylist with that track and wedding_id W Then the file is left out, and alone it gives 400 NO_TRACKS (fixed defect RISK-28; rekord-api exports it, Blocklist.java:62-68; PIN-14-0419, PIN-14-0510).
- [ ] #8 Given W and a tagged file "Substitution" by "Other Band" When the DJ calls exportPlaylist with that track and wedding_id W Then it is exported, because the fix of RISK-28 applies only to a track whose normalised artist is empty and that track's artist differs from the entry's.
- [ ] #9 Given a wedding whose never list holds ("Avicii", "Levels") When the DJ calls exportMissing with that wedding_id and the tracks ("Avicii", "Levels (Radio Edit)") and ("Prince", "Purple Rain") Then the answer is 200, the list names Prince and does not name Avicii, and the header counts 1 track (LibraryExportTest.java:168-184, ExportsResource.java:111-122).
- [ ] #10 Given the same wedding When the DJ calls exportMissing with only ("", "Levels") and then with only ("Avicii", "Levels") Then each answer is 400 NO_TRACKS with the message "Nothing is missing.", the first because of the RISK-28 fix (ExportsResource.java:123-125) (fixed defect RISK-28; rekord-api answers 200 with the line for ("", "Levels"), Blocklist.java:62-68).
- [ ] #11 Given a never-list entry with artist "", title "Levels" and free_text "levels by avicii" When the DJ calls exportMissing with only ("Avicii", "Levels (Radio Edit)") Then the answer is 400 NO_TRACKS with the message "Nothing is missing.", because the entry is keyed on its title and blocks "Levels" from every artist (Blocklist.java:44-68).
- [ ] #12 Given a DJ of business 1 whose active library holds "Levels" by Avicii and a track x, the never list ("Avicii", "Levels") on a wedding of business 2, on a business 1 wedding whose slots the DJ does not fill and on a soft-deleted wedding the DJ was assigned to, and a random UUID When the DJ calls exportPlaylist with both tracks and exportMissing with ("Avicii", "Levels") once with each of the four ids Then each of the 8 answers is 404 NO_WEDDING "There is no such wedding." with no file, never 422 VALIDATION_FAILED (deviation UD-19.m7 over UD-19.i2; rekord-api answers 200 and applies each existing wedding's never list, ExportsResource.java:150-152).
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
