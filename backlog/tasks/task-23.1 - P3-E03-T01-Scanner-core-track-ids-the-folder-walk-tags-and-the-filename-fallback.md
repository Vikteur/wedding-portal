---
id: TASK-23.1
title: >-
  P3-E03-T01 Scanner core: track ids, the folder walk, tags and the filename
  fallback
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.1
references:
  - 'rekord-api/src/main/java/app/rekord/scanner/TagReader.java:37-200'
  - 'rekord-api/src/main/java/app/rekord/scanner/LibraryWalk.java:33-87'
  - 'rekord-api/src/main/java/app/rekord/scanner/FilenameParse.java:13-40'
  - 'rekord-backend/server/scanner/tags.py:38-45'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:122-138'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:488'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:512'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-23
priority: high
type: feature
ordinal: 30301
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want every audio file of my music folder read into a track with its artist, title, tempo and key so that matching knows what I own.

The pieces of a folder scan that do not depend on a request, ported from rekord-api's scanner package and tested at unit level: the track id (BR-LIB-12), the walk (BR-LIB-18), tag reading (BR-LIB-21, BR-LIB-22) and the filename parse for files without a usable title (BR-LIB-23). The track id must equal the Python and Java algorithm, because remembered choices and imported playlists name tracks by it. The walk keeps following symbolic links, and visits each real directory once so a link loop ends (PIN-13-0488); rekord-api has no loop guard (LibraryWalk.java:43-75). The filename parse follows rekord-api where rekord-backend differs (analysis 13 §7). The scan job that uses these pieces is P3-E03-T02.

- Covers: BR-LIB-12, BR-LIB-18, BR-LIB-21, BR-LIB-22, BR-LIB-23, PIN-13-0512, PIN-13-0488

Plan item `P3-E03-T01` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the path "/music/Café/Été 2026.mp3" with é and É written as the precomposed code points U+00E9 and U+00C9 (NFC) When wedding-portal computes its track id Then the id is "754f1cf7479e", the first 12 hex characters of SHA-1 over the UTF-8 bytes of the path (BR-LIB-12, PIN-13-0512; TagReader.java:68-76), and the same path in NFD gives the different id "2ee32e360549", because the SHA-1 runs over the bytes of the path as given, with no Unicode normalisation.
- [ ] #2 Given a folder holding a.mp3, B.FLAC, c.m4a, d.wav, e.aiff, f.aif, g.m4p and notes.txt, a hidden folder .cache holding h.mp3 and a subfolder Sub holding i.MP3 When wedding-portal walks the folder Then the audio list holds the 7 files a.mp3, B.FLAC, c.m4a, d.wav, e.aiff, f.aif and Sub/i.MP3, the DRM list holds g.m4p, and notes.txt and .cache/h.mp3 are in neither (BR-LIB-18).
- [ ] #3 Given the same folder walked twice When the two audio lists are compared Then they are in the same order, because the entries of each directory are sorted by path (LibraryWalk.java:52-53).
- [ ] #4 Given a folder holding x.mp3, a symbolic link to another folder holding y.mp3, and a symbolic link back to the folder itself When wedding-portal walks the folder Then the walk ends, y.mp3 is listed through the first link, and x.mp3 and y.mp3 are each listed exactly once (fixed defect PIN-13-0488; rekord-api follows the loop with no guard).
- [ ] #5 Given a folder with a subfolder the process cannot read When wedding-portal walks it Then the other files are still listed, and the walk returns one error whose text names the unreadable subfolder, which the scan reports with an empty file name (ScanService.java:186-189).
- [ ] #6 Given an mp3 whose tags hold artist "Artist A", title "Title A" and album "Album A" When wedding-portal reads it Then the track has those three values, tag_source "tags", the duration in seconds rounded half-even to 1 decimal, the bitrate in kbps, and filename equal to the file name without its extension (BR-LIB-21).
- [ ] #7 Given an mp3 named "07. Band X - Song Y.mp3" whose tags hold artist "Tag Artist" and no title When wedding-portal reads it Then artist is "Band X", title is "Song Y" and tag_source is "filename", and "Tag Artist" is discarded (BR-LIB-21).
- [ ] #8 Given a file with the extension .aif When wedding-portal reads it Then its ext is "aiff" (BR-LIB-21).
- [ ] #9 Given mp3 files whose BPM tag is "128,5", "0", "301" and "abc" When wedding-portal reads them Then bpm is 128.5, null, null and null: only values from 20 to 300 are kept, and a kept value is rounded to 2 decimals (BR-LIB-22).
- [ ] #10 Given an mp3 with the key tag "8A", a FLAC file with no KEY field and the Vorbis comment INITIALKEY=11B, an mp3 whose only key is a TXXX frame INITIALKEY=11B, and an mp3 whose key tag holds 20 characters When wedding-portal reads them Then musical_key is "8A", "11B", null and the first 16 characters (BR-LIB-22; TagReader.java:173-184).
- [ ] #11 Given the file names "01 Artist A - Title A", "2Pac - Changes", "Artist_B – Title B", "X — Y - Z", "123. Only Title" and "1234 Numbers" When wedding-portal parses them Then artist and title are ("Artist A", "Title A"), ("2Pac", "Changes"), ("Artist B", "Title B"), ("X — Y", "Z"), (null, "Only Title") and (null, "1234 Numbers") (BR-LIB-23; FilenameParse.java:15-40).
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
