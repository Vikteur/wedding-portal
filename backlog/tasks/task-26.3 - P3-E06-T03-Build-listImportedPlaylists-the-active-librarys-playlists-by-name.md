---
id: TASK-26.3
title: 'P3-E06-T03 Build listImportedPlaylists: the active library''s playlists by name'
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-26.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:183-188'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:232-258'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryMapper.java:114-133'
  - 'rekord-contract/paths/dj.yaml:294-308'
  - 'rekord-contract/components/library.yaml:322-381'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:167'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:440'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30603
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see the playlists imported into the library I work in so that I can pick one to narrow matching to or remove one I no longer use.

Lists the imported playlists of the active library ordered by name, each with id, library_id, name, added_at, track_count and missing_count; there is no list without an active library (BR-LIB-41, LibraryResource.java:183-188). Playlist track rows have no foreign key to tracks and are kept when a rescan drops a file, so they rebind when the file returns (P3-E02-T01). track_count counts those rows, including rows whose file is gone, while getImportedPlaylistTracks lists only files that exist (RISK-36, analysis 13 R8); the user keeps rekord-api's count (UD-19.m4).

- Builds: `listImportedPlaylists`
- Covers: RISK-36, UD-19.m4
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion

Plan item `P3-E06-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library L with imported playlists named "Warmup", "Closing" and "Most played" When the DJ calls listImportedPlaylists Then the answer is 200 with playlists ordered Closing, Most played, Warmup, each with id, library_id L, name, added_at, track_count and missing_count (LibraryService.java:232-258).
- [ ] #2 Given an imported playlist of the DJ's library M, not the active one, and an imported playlist of another DJ's library When the DJ calls listImportedPlaylists with L active Then neither is listed.
- [ ] #3 Given an active library with no imported playlist When the DJ calls listImportedPlaylists Then the answer is 200 with playlists []; and given a DJ with no active library the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
- [ ] #4 Given a playlist of L imported with f1, f2 and f3, and a rescan of L that no longer finds f2, which no other library holds When the DJ calls listImportedPlaylists Then the playlist's track_count is still 3 (UD-19.m4, RISK-36; LibraryService.java:242-258).
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
