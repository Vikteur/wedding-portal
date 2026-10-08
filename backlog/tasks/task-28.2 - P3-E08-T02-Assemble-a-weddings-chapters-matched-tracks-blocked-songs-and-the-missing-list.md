---
id: TASK-28.2
title: >-
  P3-E08-T02 Assemble a wedding's chapters: matched tracks, blocked songs and
  the missing list
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-28.1
  - TASK-21.3
  - TASK-24.6
  - TASK-25.2
  - TASK-27.5
references:
  - 'rekord-backend/server/main.py:538-593'
  - 'rekord-backend/server/export/couple.py:15-58'
  - 'rekord-backend/server/couples.py:107-115'
  - 'rekord-backend/tests/test_couple_export.py:96-172'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:167-171'
  - 'rekord-backend/server/couples.py:568-577'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:43-54'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:72-77'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-28
priority: high
type: feature
ordinal: 30802
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want each chapter of a wedding matched against my active library with the couple's never list applied so that only confident picks reach the decks and every other song lands on one shopping list.

The service behind the three operations of P3-E08-T01, built in Java from the POC (rekord-backend/server/main.py:538-593, export/couple.py:15-58). It reads the wedding's song lists as getDjSongLists does (P3-E01-T03), turns each entry into matcher input (BR-MX-39), matches each chapter in the order of the night against the whole active library with its remembered choices and playlist membership (BR-MX-37), and keeps, per entry, the track the result names in auto_selected_id, whatever the bucket, so a remembered choice that preselects a file in an ambiguous result (UD-19.c) puts that file in the chapter (main.py:572-576); every other song goes to one missing list de-duplicated over all chapters, and never-listed songs are counted as blocked (BR-MX-38). It writes nothing.

- Covers: BR-MX-37, BR-MX-38, BR-MX-39, PIN-20-0092, UD-19.c

Plan item `P3-E08-T02` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding whose opening_dance and couple_top20 lists each hold an entry whose result has auto_selected_id set to a library track, and whose other lists are empty When the chapters are assembled Then the playlists are "01 Opening dance" and "02 Their top 20", and with only the couple_top20 entry the single playlist is "01 Their top 20": a chapter in which no result has auto_selected_id set is dropped and numbering counts only the chapters kept (BR-MX-37; test_couple_export.py:96-131).
- [ ] #2 Given entries in every list kind When the chapters are assembled Then the chapters run in the order opening_dance, second_third, couple_top20, friends_top20, must_plays, playlist_links, named with a two-digit position, a space and the labels "Opening dance", "Second & third song", "Their top 20", "Friends' top 20", "Must-plays" and "Playlist links", and the never list is a filter, not a chapter (couple.py:15-29, couples.py:107-115); the labels and the order are fixed in wedding-portal as the POC's and are not read from song_list_kinds, so must_plays is named "Must-plays" although its kind label is "Must-plays, no matter what".
- [ ] #3 Given entries with title "Levels" and artist "", and with title "the song from our road trip" that the song-list write copied from free_text "the song from our road trip" When they become matcher input Then the first is (artist "", title "Levels") and the second is (artist "", title "the song from our road trip"), and an entry row whose title is empty, inserted straight into the test database, is neither matched, missing nor blocked (BR-MX-39; couple.py:37-58; SongService.java:43-54).
- [ ] #4 Given entries with duration_ms 215000, 0 and null When they become matcher input Then their durations are 215.0 seconds, none and none, and the entries keep their list order (couple.py:49-57).
- [ ] #5 Given an active library with remembered choices and imported playlists When a chapter is matched Then each entry gets the result matchTracks of P3-E04-T06 gives for the same input without playlist_id, so the whole library is searched and a remembered choice of P3-E05-T02 counts, and the playlist takes, in entry order, the track each result names in auto_selected_id, whatever the bucket, and nothing for a result whose auto_selected_id is null (BR-MX-38, UD-19.c; main.py:561-587).
- [ ] #6 Given a chapter entry whose result has auto_selected_id null When the chapters are assembled Then it is added to the missing list with its artist, its title and had_candidates true exactly when the result has at least one candidate (main.py:575-586).
- [ ] #7 Given the same song ("Ghost Act", "A Song Nobody Owns"), absent from the library, in couple_top20 and in friends_top20 When the chapters are assembled Then the missing list holds it once: entries are de-duplicated over all chapters by artist and title, stripped and lower-cased, and the first occurrence is kept (test_couple_export.py:157-172).
- [ ] #8 Given a never list holding ("Avicii", "Levels") and an entry "Levels (Radio Edit)" by Avicii in couple_top20 that the library holds When the chapters are assembled Then the entry is in no playlist and not in the missing list, and blocked counts 1 for each such entry in each chapter, using the never-list rule of P3-E07-T05 on the entry's own artist and title (main.py:568-571; test_couple_export.py:134-154).
- [ ] #9 Given a never list holding ("Avicii", "Levels") and a couple_top20 entry with artist "" and title "Levels" When the chapters are assembled Then the entry is in no playlist and not in the missing list, and blocked counts 1 (fixed defect RISK-28; the POC lists it as missing, couples.py:568-577).
- [ ] #10 Given one library track named in auto_selected_id by the result of an entry of couple_top20 and by an entry of must_plays, and by two entries of friends_top20 When the chapters are assembled Then the track is in all three playlists, twice in the friends' playlist (main.py:587-590; PIN-20-0092).
- [ ] #11 Given an active library holding one file tagged "Avicii" / "Levels", an empty never list and a couple_top20 entry with artist "" and title "Levels" When the chapters are assembled Then the entry's result is ambiguous with auto_selected_id null, and the entry is in no playlist and is in the missing list with had_candidates true, because a query without an artist is never auto (deviation UD-19.c; rekord-api's matcher auto-selects the file).
- [ ] #12 Given an active library holding one file f tagged "Avicii" / "Levels", a remembered choice of f for artist "" and title "Levels", an empty never list and a wedding whose only entry is a couple_top20 entry with artist "" and title "Levels" When the chapters are assembled Then the entry's result is ambiguous with auto_selected_id f and from_preference true, the single playlist "01 Their top 20" holds f, and the missing list is empty (UD-19.c, a remembered choice still preselects; main.py:572-576).
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
