---
id: TASK-24.5
title: >-
  P3-E04-T05 Match index cache: four scopes, playlist narrowing and invalidation
  at scan end
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - technical
  - P3
milestone: m-3
dependencies:
  - TASK-24.2
  - TASK-23.2
  - TASK-23.4
  - TASK-22.5
  - TASK-22.7
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryIndexCache.java:28-155'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:88-160'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:78-130'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryService.java:282-290'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:216-232'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:175-176'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:432'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: task
ordinal: 30405
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the match index of a library built once and dropped after every write so that matching stays quick and never answers from files that are gone.

The port of LibraryIndexCache. An index is cached per library and optional imported playlist in a 4-entry least-recently-used map in process memory (BR-LIB-44), built from the library's tracks in path order (P3-E04-T02) and, for a playlist scope, narrowed to the playlist's track ids. Remembered choices and playlist membership are not cached and are read from the database on every match. Every write to a library's files drops all its scopes (BR-LIB-45). rekord-api drops them when a scan starts but not when it ends, so an index built while the scan ran stays cached afterwards and misses the new files; wedding-portal also drops them when a scan ends (fixed defect RISK-25). Importing and deleting a playlist drop them too, which the tickets of P3-E06 assert. A build that races an invalidation is P3-E09-T02.

- Covers: BR-LIB-44, BR-LIB-45, RISK-25, PIN-13-0486

Plan item `P3-E04-T05` (technical,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a library L whose unscoped index was built once When wedding-portal asks for the index of L again with no write in between Then the same index is returned and the library's tracks are read from the database once (BR-LIB-44; LibraryIndexCache.java:64-88).
- [ ] #2 Given the indexes of 4 scopes built in the order S1, S2, S3 and S4, and S1 read again When the index of a fifth scope S5 is built Then the index of S2 is the one dropped, and S1, S3, S4 and S5 are returned without a rebuild (BR-LIB-44; LibraryIndexCache.java:32-45).
- [ ] #3 Given a library L holding the files f1, f2 and f3 and an imported playlist P of L whose rows name f3, f1 and a track id that L does not hold When wedding-portal builds the index of L narrowed to P Then it holds f1 and f3 in library order and nothing else (BR-LIB-43; LibraryIndexCache.java:78-96).
- [ ] #4 Given cached indexes for (L, no playlist), (L, P) and (M, no playlist) When the index of L is invalidated Then both scopes of L are rebuilt on the next request and the index of M is not (BR-LIB-45; LibraryIndexCache.java:105-109).
- [ ] #5 Given a cached index of the DJ's library L When the DJ calls deleteLibrary for L, removeSource on a source of L, startScan for L or importRekordboxXml into L Then after each call the next request for the index of L rebuilds it (BR-LIB-45; LibraryResource.java:94, :107, :119, :155; LibraryService.java:84).
- [ ] #6 Given a scan of L that adds a file and an index of L built while that scan ran When the scan reaches state done Then the cache holds no index of L, and the same holds when a scan ends in state error (fixed defect RISK-25; rekord-api invalidates only when the scan starts, LibraryResource.java:119) (PIN-13-0486).
- [ ] #7 Given a cached index of L When a remembered choice and a playlist row are added for L without an invalidation Then the remembered choices of L map signature ids to track ids including the new one, and the membership of L lists for each track id the names of L's imported playlists holding it in name order, including the new row (LibraryIndexCache.java:117-155).
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
