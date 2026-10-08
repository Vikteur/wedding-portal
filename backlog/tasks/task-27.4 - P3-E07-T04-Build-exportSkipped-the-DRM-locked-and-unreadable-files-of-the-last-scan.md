---
id: TASK-27.4
title: >-
  P3-E07-T04 Build exportSkipped: the DRM-locked and unreadable files of the
  last scan
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-27.1
  - TASK-23
references:
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:132-148'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:135-149'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:185-193'
  - 'rekord-api/src/test/java/app/rekord/scanner/ScanTest.java:245-257'
  - 'rekord-contract/paths/dj.yaml:647-667'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:428'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-27
priority: high
type: feature
ordinal: 30704
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to download the list of DRM-locked and unreadable files from my last scan so that I know which tracks to buy again and which files to repair.

The operation GET /api/export/skipped reads the DRM files and the errors that the last scan of the active library recorded (P3-E03) and answers the skipped-list body of P3-E07-T01 as the download "skipped.txt" (ExportsResource.java:132-148). The scan state lives in memory, as in rekord-api: it is empty after wedding-portal starts and holds the latest scan of each library (ScanService.java:135-149, analysis 14:428). Every DRM file is listed and the count is the number listed, with no cap (BR-LIB-26).

- Builds: `exportSkipped`
- Covers: BR-LIB-26, PIN-14-0505
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the export writes no rows, so no race of it reaches this code

Plan item `P3-E07-T04` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and an active library whose last scan stepped over a DRM file When a characterization test calls exportSkipped with no Accept header and records the status, the Content-Type (media type and charset) and the Content-Disposition Then the same test passes against wedding-portal, so the Content-Type is the one rekord-api sends, with the media type text/plain, the only one the pinned contract declares for the 200 response of exportSkipped (PIN-14-0505; paths/dj.yaml:663).
- [ ] #2 Given an active library whose last scan of P3-E03 walked a folder holding iTunes/old-purchase.m4p When the DJ calls exportSkipped Then the answer is 200, the body is the skipped list of P3-E07-T01 whose folder line names the scanned folder and whose DRM section holds the full path of old-purchase.m4p with the count 1, and the Content-Disposition is attachment; filename="skipped.txt"; filename*=UTF-8''skipped.txt (BR-LIB-26; ScanTest.java:245-257).
- [ ] #3 Given a last scan that recorded a per-file error and a walk error, the walk error with an empty file When the DJ calls exportSkipped Then the unreadable section lists the per-file error as its path and message separated by a tab and the walk error as "(folder)" and its message separated by a tab, in the order the scan recorded them (ScanService.java:185-193, :240-252).
- [ ] #4 Given a last scan that found 2,500 DRM files When the DJ calls exportSkipped Then all 2,500 paths are listed, the DRM header shows 2500 and no "...and N more, not listed." line is written, because the total passed is the number of files held (ExportsResource.java:146).
- [ ] #5 Given an active library whose last scan found no DRM file and recorded no error When the DJ calls exportSkipped Then the answer is 400 NOTHING_SKIPPED with the message "The last scan read everything it found." (ExportsResource.java:140-143).
- [ ] #6 Given an active library that has not been scanned since wedding-portal started, and a library scanned before a restart When the DJ calls exportSkipped Then each answer is 400 NOTHING_SKIPPED, because the scan state is held in memory only (ScanService.java:135-149).
- [ ] #7 Given a DJ with no active library When the DJ calls exportSkipped Then the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
- [ ] #8 Given the step of exportSkipped that builds the file, called at unit level with a scan status that holds no folder, the active library name "Main" and one DRM path, a state no API call reaches because startScan stores the folder before it registers the job (ScanService.java:109-125) When it builds the file Then the second line is "# Folder: Main" (ExportsResource.java:145, Exports.java:223).
- [ ] #9 Given a DJ who downloads the skipped list When wedding-portal answers exportSkipped Then the body is written straight into the response and no file is created under the temporary directory of the JVM (fixed defect, analysis 14-dj-matching-exports.md:427: export temp files hold paths and names for up to 1 h; rekord-api writes each body to java.io.tmpdir/rekord-exports and sweeps it after 1 h, ExportsResource.java:49-50, :173-185, :210-240).
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
