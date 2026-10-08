---
id: TASK-26
title: P3-E06 Imported playlists and the Spotify playlist fetch
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-24
references:
  - rekord-api/src/main/java/app/rekord/library/PlaylistImport.java
  - rekord-api/src/main/java/app/rekord/library/LibraryService.java
  - rekord-api/src/main/java/app/rekord/spotify/SpotifyPlaylistFetch.java
  - rekord-api/src/main/java/app/rekord/spotify/SpotifyEmbed.java
  - docs/rewrite/analysis/13-dj-library-ingestion.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30600
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to import my existing playlists and read a public Spotify playlist so that the songs I already play rank higher and a playlist link becomes a list to match.

Goal: the playlist inputs of matching. Scope: the operations listImportedPlaylists, importPlaylist, deleteImportedPlaylist (test-first), getImportedPlaylistTracks (test-first) and fetchSpotifyPlaylist (test-first); format sniffing, entry shapes, names, resolution through the matcher at a score of 0.75 or more, re-import by name and the result counts (BR-LIB-33 to BR-LIB-41); the accepted Spotify inputs, the anonymous embed fetch, parsing and truncation (BR-LIB-49 to BR-LIB-52), tested against WireMock; playlist tracks that shrink after a rescan (RISK-36). It follows the matcher because playlist resolution uses it. Out of scope: exports (P3-E07).

Plan item `P3-E06` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library with tracks When the DJ calls importPlaylist with an M3U file whose entries resolve Then listImportedPlaylists lists the playlist and getImportedPlaylistTracks returns its tracks in import order.
- [ ] #2 Given an imported playlist When the DJ imports a playlist with the same name in another letter case Then the tracks of the existing playlist are replaced and no second playlist exists.
- [ ] #3 Given a Spotify playlist link and an upstream embed page served by a WireMock stub When the DJ calls fetchSpotifyPlaylist Then the answer lists the title, artists and duration of each track the page holds, and an unreachable upstream answers 502 SPOTIFY_FETCH_FAILED.
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
