---
id: TASK-27.1
title: >-
  P3-E07-T01 Export file builders: m3u8, rekordbox XML, missing list and skipped
  list
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-23
references:
  - 'rekord-api/src/main/java/app/rekord/library/Exports.java:20-255'
  - 'rekord-api/src/test/java/app/rekord/library/ExportsTest.java:24-186'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:152-158'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-27
priority: high
type: feature
ordinal: 30701
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want the exported files written the way rekordbox and a shop search box read them so that the night loads without a track turning up as missing.

Pure builders (no database, no HTTP) that turn tracks into the four export bodies of rekord-api (Exports.java:48-255). The m3u8 lists each path verbatim as scanned (BR-MX-27). The rekordbox XML holds a collection and one playlist node (BR-MX-28); each Location is the path as a file://localhost/ URL with a Windows drive letter kept verbatim (BR-MX-29); each Kind comes from the extension (BR-MX-30). The missing list splits what was not found from what was turned down; the skipped list splits DRM-locked files from unreadable ones (BR-MX-33). Every body is UTF-8 with LF line ends and no byte-order mark. P3-E07-T02 to P3-E07-T04 put these builders behind the three export operations.

- Covers: BR-MX-27, BR-MX-28, BR-MX-29, BR-MX-30, BR-MX-33

Plan item `P3-E07-T01` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the tracks (C:\Music\Dreams.mp3, ext mp3, artist "Fleetwood Mac", title "Dreams", duration 257.3) and (C:\Music\unknown.wav, ext wav, no artist, title "unknown", no duration) When the m3u8 builder runs Then the body is exactly the five lines "#EXTM3U", "#EXTINF:257,Fleetwood Mac - Dreams", "C:\Music\Dreams.mp3", "#EXTINF:-1,unknown" and "C:\Music\unknown.wav", each ended by LF, with no byte-order mark (BR-MX-27; ExportsTest.java:136-152).
- [ ] #2 Given tracks with durations 0, 256.5 and 256.4 and a track whose artist is the empty string When the m3u8 builder runs Then the #EXTINF seconds are -1, 257 and 256 (Math.round, half up), and the label of the artist-less track is its title alone (Exports.java:48-61).
- [ ] #3 Given the paths C:\Música\Té st.mp3, /home/dj/Music/a b.mp3 and C:\Music\a+b\100% Pure.wav When the location rule runs Then the Locations are file://localhost/C:/M%C3%BAsica/T%C3%A9%20st.mp3, file://localhost/home/dj/Music/a%20b.mp3 and file://localhost/C:/Music/a%2Bb/100%25%20Pure.wav: backslashes become /, leading slashes are dropped, a two-character drive segment stays verbatim and every other byte outside letters, digits and !'()*~-._ is percent-encoded from UTF-8 in upper case (BR-MX-29; Exports.java:73-102).
- [ ] #4 Given each path of ExportsTest.java:24-58 When it is turned into a Location and read back by the location reader of the rekordbox collection import of P3-E03 Then the path read back equals the original path (ExportsTest.java:24-58).
- [ ] #5 Given the extensions mp3, m4a, flac, wav, aiff, ogg, the empty string and no extension When the kind rule runs Then the kinds are "MP3 File", "M4A File", "FLAC File", "WAV File", "AIFF File", "OGG File", "Unknown" and "Unknown" (BR-MX-30; Exports.java:111-117, ExportsTest.java:168-174).
- [ ] #6 Given the playlist name "Emma & Julian" and two tracks, the first with duration 257.3 and the second with no artist, no album and no duration When the XML builder runs Then the body starts with <?xml version="1.0" encoding="UTF-8"?>, then DJ_PLAYLISTS Version="1.0.0" holding PRODUCT Name="rekordbox" Version="6.8.5" Company="AlphaTheta" and COLLECTION Entries="2" (BR-MX-28; Exports.java:127-166).
- [ ] #7 Given the same input When the XML builder runs Then each COLLECTION TRACK carries, in this order, TrackID 1 then 2, Name, Artist, Album, Kind, TotalTime="257" on the first track only and Location; a missing artist or album is written as the empty string; and PLAYLISTS holds NODE Type="0" Name="ROOT" Count="1" holding NODE Name="Emma &amp; Julian" Type="1" KeyType="0" Entries="2" with TRACK Key="1" and TRACK Key="2" (Exports.java:127-166).
- [ ] #8 Given a title, artist, album or playlist name holding the characters & < > and " When the XML builder runs Then each is written as &amp; &lt; &gt; and &quot;, and the collection import of P3-E03 reads the body back to the same paths (Exports.java:119-125, ExportsTest.java:60-81).
- [ ] #9 Given the playlist name "Emma & Julian", the library name "Studio PC" and the missing tracks ("Avicii", "Levels", had_candidates false) and ("Prince", "Purple Rain", had_candidates true) When the missing-list builder runs Then the lines are "# Missing tracks", "# Playlist: Emma & Julian", "# 2 track(s) from this playlist are not in Studio PC.", "", "# Not found:", "Avicii - Levels", "", "# Skipped — these had possible matches you didn't take:", "Prince - Purple Rain" and "", joined by LF (ExportsTest.java:154-166).
- [ ] #10 Given missing tracks that all have had_candidates false, one of them with the artist " " and the title " Levels " When the missing-list builder runs Then no "# Not found:" line and no skipped section is written, artist and title are stripped, and that track's line is "Levels" alone; and with no library name the third line ends "are not in your library." (Exports.java:176-212).
- [ ] #11 Given the folder D:\Music, the DRM files [D:\Music\locked.m4p] with total 1 and the errors (D:\Music\broken.mp3, "corrupt header") and ("", "walk denied") When the skipped-list builder runs Then the lines are "# Files skipped by the last scan", "# Folder: D:\Music", "", "# DRM-protected — rekordbox cannot play these (1)", the two explanation lines of Exports.java:228-229 word for word, the locked file, "", the unreadable header of Exports.java:239-240 with the count 2, the broken file and its message separated by a tab, "(folder)" and "walk denied" separated by a tab, and "", joined by LF (BR-MX-33).
- [ ] #12 Given no DRM files and no errors, and separately one DRM file with a total of 3 and an error whose message is blank When the skipped-list builder runs Then each empty section holds the single line "# (none)", the DRM section ends with "# ...and 2 more, not listed.", and the blank-message error is listed as its file alone (Exports.java:219-255, ExportsTest.java:176-186).
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
