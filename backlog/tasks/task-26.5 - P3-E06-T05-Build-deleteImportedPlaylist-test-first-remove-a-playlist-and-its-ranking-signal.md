---
id: TASK-26.5
title: >-
  P3-E06-T05 Build deleteImportedPlaylist test-first: remove a playlist and its
  ranking signal
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - test-first
milestone: m-3
dependencies:
  - TASK-26.3
  - TASK-26.4
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:198-204'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:260-290'
  - 'rekord-contract/paths/dj.yaml:346-367'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:167'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30605
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to remove an imported playlist so that it no longer ranks files or appears as a filter for matching.

Deletes one imported playlist with its track rows by cascade (P3-E02-T01) and answers the remaining playlists of the playlist's library (LibraryResource.java:198-204). The same visibility rule as P3-E06-T04 applies, and no active library is needed. The delete invalidates the index cache of the library (LibraryService.java:283-290, P3-E04-T05). rekord-api has no test for this operation, so a characterization test runs first (UD-15.d).

- Builds: `deleteImportedPlaylist`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-LIB-41
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E06-T05` (user-story,P3,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and a DJ whose library L holds the playlists "Closing" and "Warmup" When a characterization test calls deleteImportedPlaylist for "Warmup", again for the same id and for a playlist of another DJ's library Then it records 200 with playlists holding only "Closing", 404 NO_PLAYLIST and 404 NO_LIBRARY, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given L with the playlists "Closing" and "Warmup" When the DJ calls deleteImportedPlaylist for "Warmup" Then the answer is 200 with playlists holding only "Closing", and the track rows of "Warmup" are gone while the files stay in L.
- [ ] #3 Given a playlist of the DJ's library M, which is not the active one When the DJ calls deleteImportedPlaylist for it Then the answer is 200 with the remaining playlists of M.
- [ ] #4 Given a playlist of another DJ's library and an unknown playlist id When the DJ calls deleteImportedPlaylist with each Then the answers are 404 NO_LIBRARY with the message "That library does not exist." and 404 NO_PLAYLIST with the message "That playlist is gone.", and the other DJ's playlist still exists.
- [ ] #5 Given a DJ who called matchTracks with playlist_id P and saw P's name in a candidate's playlists When the DJ deletes P and calls matchTracks with playlist_id P and then without it Then the first answer is 404 NO_PLAYLIST and in the second no candidate lists P's name (P3-E04-T05, P3-E04-T06).
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
