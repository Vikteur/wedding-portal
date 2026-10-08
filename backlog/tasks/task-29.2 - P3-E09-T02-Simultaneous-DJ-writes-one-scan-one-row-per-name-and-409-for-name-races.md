---
id: TASK-29.2
title: >-
  P3-E09-T02 Simultaneous DJ writes: one scan, one row per name, and 409 for
  name races
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-29.1
  - TASK-22.3
  - TASK-22.4
  - TASK-22.6
  - TASK-23.2
  - TASK-23.4
  - TASK-24.5
  - TASK-26.2
references:
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:109-129'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryIndexCache.java:60-92'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:148-166'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:233'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:435-436'
  - 'docs/rewrite/business-analysis.md:1463'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-29
priority: high
type: feature
ordinal: 30902
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want a double click or two open tabs to give me one library, one scan and one playlist so that no duplicate lands in my library.

Settles RISK-48 (analysis 13 R3 and R4) under the race policy of P0-E05-T05. ScanService.start checks for a running scan and then registers the new one without a lock, so two simultaneous starts both run (ScanService.java:109-129); wedding-portal runs the check and the registration under one in-process lock. LibraryIndexCache.indexOf builds outside its lock, so an invalidation that lands during a build is overwritten by the stale index (LibraryIndexCache.java:64-88). The unique indexes on library names, sources, imported playlist names and settings are reached by a pre-check followed by an insert, so the loser of a race gets the 500 of the catch-all: selectLibrary writes the settings row with INSERT … ON CONFLICT, and importRekordboxXml and importPlaylist lock the libraries row (SELECT … FOR UPDATE) before they look up the source or the playlist by name, so each of these gives both calls the answers of a sequential pair; createLibrary and renameLibrary have no row to lock before the name check, so the loser of their name race meets the unique index and answers 409 DUPLICATE_NAME with the message "You already have a library called that.", the answer of the name check (deviation UD-19.i5, RISK-26; rekord-api answers 500 UNKNOWN, 41 D-3, 41-domain-model-and-glossary.md:1608-1616). The library-name pre-check that uses the caller as owner is fixed in P3-E02-T04. Criteria 1 and 2 make both calls pass the name check with a test latch in the library repository adapter; criteria 3 to 6 start both calls behind one barrier and repeat the pair 20 times; the seventh holds the index build at a latch between reading the tracks and storing the index while the scan invalidates the library.

- Covers: RISK-48, UD-19.i5

Plan item `P3-E09-T02` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ with no library named "Studio PC" and a test latch in the library repository adapter that holds two createLibrary calls with name "Studio PC" after the name check until both have passed it When the DJ sends the two calls Then one answers 201 and the other 409 DUPLICATE_NAME with the message "You already have a library called that.", and the business holds one library of the DJ named "Studio PC" (deviation UD-19.i5; rekord-api answers 500 UNKNOWN to the loser).
- [ ] #2 Given a planner, the unowned libraries "Old" and "Older" of one business and the test latch of criterion 1 holding both renameLibrary calls after the name check When the planner sends renameLibrary of "Old" to "Gig" and of "Older" to "gig" Then one answers 200 and the other 409 DUPLICATE_NAME with the message "You already have a library called that.", and exactly one of the two libraries is named "Gig" or "gig" (deviation UD-19.i5; rekord-api answers 500 UNKNOWN to the loser).
- [ ] #3 Given a DJ owning libraries A and B and no settings row When the DJ sends selectLibrary on A and selectLibrary on B at the same time Then both answer 200, and one settings row for the DJ and business holds A or B, because the settings row is written with INSERT … ON CONFLICT (fixed defect RISK-48; rekord-api answers 500 UNKNOWN to the losing request).
- [ ] #4 Given a library A with no running scan and a folder holding 3 audio files When the DJ sends two startScan calls for A with that folder at the same time Then one answers 202 and the other 409 SCAN_IN_PROGRESS with the message "A scan of this library is already running.", one scan runs, and A has one folder source for the folder, because the check for a running scan and the registration of the new one run under one in-process lock (fixed defect RISK-48; ScanService.java:113-124).
- [ ] #5 Given an active library When the DJ sends two importRekordboxXml calls with the same file and the name "Gig.xml" at the same time Then both answer 200, and the library has one xml source labelled "Gig.xml" holding the file's tracks, because each import locks the libraries row before it looks up the source by label (fixed defect RISK-48; rekord-api answers 500 UNKNOWN to the losing request).
- [ ] #6 Given an active library L When the DJ sends two importPlaylist calls with the same M3U file and the name "Most played.m3u8" at the same time Then both answer 200, and L has one imported playlist named "Most played" whose tracks are those of the file, because each import locks the libraries row before it looks up the playlist by name (fixed defect RISK-48; rekord-api answers 500 UNKNOWN to the losing request).
- [ ] #7 Given a build of the index of L that has read the tracks of L, and a scan of L that adds a file and invalidates the index of L before that build stores its result When the next matchTracks call for L runs Then its candidates include the added file, because an index built before an invalidation of its library is never stored after it (fixed defect RISK-48; LibraryIndexCache.java:64-88).
- [ ] #8 Given the race tests of criteria 1 to 6 of this ticket, with the pairs of criteria 3 to 6 repeated 20 times When they run in CI Then no call answers 500, and the stored rows match the criterion after every run.
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
