---
id: TASK-26.1
title: >-
  P3-E06-T01 Playlist file reader: decoding, format sniffing, entry shapes and
  the playlist name
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-23
references:
  - 'rekord-api/src/main/java/app/rekord/library/PlaylistImport.java:31-277'
  - 'rekord-api/src/test/java/app/rekord/library/PlaylistImportTest.java:20-175'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:159-162'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30601
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to upload the playlist file rekordbox exported in whichever format it wrote so that my existing sets count when the matcher ranks versions.

A pure reader (no database) that turns the uploaded bytes into a name and a list of entries, each a path where the format carries one plus an artist and a title (PlaylistImport.java:31-277). Bytes are decoded by byte-order mark, then strict UTF-8, then Latin-1, which cannot fail, so a file with wrong accents still imports (BR-LIB-33). The format is sniffed from the content, never from the extension (BR-LIB-34). M3U carries a path and an optional #EXTINF "Artist - Title" label, PLS carries paths only, XML carries a path plus the Artist and Name attributes, and the rekordbox TXT export carries columns, of which a title column is required (BR-LIB-35). The name is the upload's file name without its extension (BR-LIB-36). The XML reader resolves no DTD and no external entity. P3-E06-T02 puts this reader behind importPlaylist.

- Covers: BR-LIB-34, BR-LIB-35, BR-LIB-36

Plan item `P3-E06-T01` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the bytes of one M3U playlist holding the line "#EXTINF:1,Café - Crème" followed by the line "C:\a.mp3", encoded as UTF-16LE with a byte-order mark, as UTF-16BE with a byte-order mark, as UTF-8 with a byte-order mark, as UTF-8 without one, and as Latin-1, which is not valid UTF-8 When the reader decodes and parses each Then each of the first four gives the single entry (path "C:\a.mp3", artist "Café", title "Crème"), and the Latin-1 file gives that same entry and no error (BR-LIB-33; PlaylistImport.java:65-81, PlaylistImportTest.java:166-175).
- [ ] #2 Given an upload whose text after leading whitespace starts with "<?xml" or "<DJ_PLAYLISTS", one that starts with "[PLAYLIST]" in any letter case, one whose first line holds a tab, and any other text When the reader sniffs each Then it reads them as XML, PLS, rekordbox TXT and M3U, and an M3U uploaded with the name "set.txt" is still read as M3U (BR-LIB-34; PlaylistImport.java:91-115).
- [ ] #3 Given an M3U with "#EXTINF:257,Fleetwood Mac - Dreams" before "C:\Music\dreams.mp3" and "#EXTINF:-1,No Artist Here" before "C:\Music\mystery.mp3" When the reader parses it Then the entries are (C:\Music\dreams.mp3, "Fleetwood Mac", "Dreams") and (C:\Music\mystery.mp3, "", "No Artist Here"); a path line with no #EXTINF before it gets artist "" and title "", other lines starting with # are skipped, and the separators " - ", " – " and " — " all split a label (PlaylistImportTest.java:20-41).
- [ ] #4 Given an M3U line "file://localhost/C:/M%C3%BAsica/T%C3%A9%20st.mp3" When the reader parses it Then the entry's path is "C:\Música\Té st.mp3", converted the way a rekordbox XML location is (P3-E03), and a file:// value in a PLS line or a TXT location column is converted the same way (PlaylistImportTest.java:42-54).
- [ ] #5 Given a PLS with File1=C:\Music\one.mp3, Title1=One and File2=C:\Music\two.mp3 When the reader parses it Then it returns 2 entries carrying only the two paths, with artist and title "" (PlaylistImportTest.java:55-70).
- [ ] #6 Given an XML export with a COLLECTION TRACK carrying Name "Dreams", Artist "Fleetwood Mac" and a Location, and a PLAYLISTS TRACK carrying only Key "1" When the reader parses it Then it returns exactly one entry with that path, artist and title; and an XML file that declares an external entity, or that is not well-formed, is refused with 400 BAD_PLAYLIST and the message "That is not valid XML." (PlaylistImportTest.java:108-144).
- [ ] #7 Given a rekordbox TXT export encoded as UTF-16LE with a byte-order mark, with the header "#\tTrack Title\tArtist\tLocation", a row for "Dreams" by "Fleetwood Mac" at C:\Music\dreams.mp3 and a row for "Été" by "Étienne" with an empty location When the reader parses it Then it returns 2 entries, the first with that path, artist and title and the second titled "Été" with no path (BR-LIB-35; PlaylistImportTest.java:72-97).
- [ ] #8 Given TXT exports whose header cells differ in letter case, carry a byte-order mark or a leading #, or name the same column twice When the reader picks the columns Then cells are compared lowercased with the mark and the # removed, the title column is the leftmost cell named track title, title, name or song, the artist column the leftmost named artist or artist name, the location column the leftmost named location, file, path or folder, and a row with an empty title is skipped (PlaylistImport.java:207-267).
- [ ] #9 Given a TXT export whose header has no title column When the reader parses it Then the answer is 400 BAD_PLAYLIST with the message "That looks like a rekordbox TXT export but has no 'Track Title' column — re-export it with the title and artist columns visible." (PlaylistImportTest.java:98-107).
- [ ] #10 Given an upload of 0 bytes and an upload of only spaces and newlines When the reader reads each Then the answer is 400 EMPTY_FILE with the message "That file is empty." (PlaylistImportTest.java:145-155).
- [ ] #11 Given a non-blank M3U holding only lines that start with # When the reader reads it Then the answer is 400 BAD_PLAYLIST with the message "No tracks found in that file. Export the playlist from rekordbox (right-click the playlist, then Export) as m3u8, txt, pls or xml." (PlaylistImport.java:109-113).
- [ ] #12 Given the upload names "Most played 2026.m3u8", "no-extension", "" and no name When the reader names the playlist Then the names are "Most played 2026", "no-extension", "Imported playlist" and "Imported playlist": surrounding spaces are stripped and one trailing extension of 1 to 5 letters or digits is removed (BR-LIB-36; PlaylistImport.java:122-126).
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
