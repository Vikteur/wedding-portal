---
id: TASK-23.2
title: >-
  P3-E03-T02 Build startScan: one background scan per library into a folder
  source
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-23.1
  - TASK-22.7
references:
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:102-274'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:113-125'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:215-260'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:361-396'
  - 'rekord-contract/paths/dj.yaml:392-418'
  - 'rekord-contract/components/library.yaml:185-243'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:130-141'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-23
priority: high
type: feature
ordinal: 30302
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to scan my music folder into the library I work in so that newly bought tracks can be matched.

A scan runs in the background and answers 202 at once (BR-LIB-17). The folder is a path on the machine running wedding-portal, stripped, made absolute and normalised, with no quote stripping, no ~ expansion and no allowed root (BR-LIB-16); the user keeps that rule, and RISK-08 and PIN-13-0487 are accepted risks (UD-19.m2). A library_id that does not exist or belongs to another business answers 422 VALIDATION_FAILED naming library_id (deviation UD-19.i2). A folder becomes one source of the library, reused on every rescan (BR-LIB-11), and a finished scan makes the source's tracks exactly what is on disk (BR-LIB-13, BR-LIB-24). Unchanged files reuse their stored row unless force is set (BR-LIB-19), changed files are parsed by a pool of 8 workers, and one unreadable file never fails the scan (BR-LIB-20). Dropping the match index when a scan starts and when it completes is P3-E04-T05; two concurrent starts are P3-E09-T02.

- Builds: `startScan`
- STOP (human approval in the pull request): auth-access
- Covers: BR-LIB-16, BR-LIB-17, BR-LIB-19, BR-LIB-20, BR-LIB-24, BR-LIB-07, BR-LIB-13, RISK-08, PIN-13-0487, UD-19.m2, UD-19.i2
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the race of two concurrent starts of one library is settled by P3-E09-T02 (202 or 409 SCAN_IN_PROGRESS, never 500)

Plan item `P3-E03-T02` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library and a folder holding 2 mp3 files and 1 m4p file When the DJ calls startScan with that folder and polls getScanStatus until state is done Then startScan answered 202 with started true and the library id, the library holds 2 tracks with the ids of P3-E03-T01, and the report has track_count 2, skipped_drm 1 and the m4p path in skipped_drm_files.
- [ ] #2 Given a folder string made of a temporary folder path followed by "/sub/.." with 2 spaces before and after When the DJ calls startScan with it Then the scan reads the temporary folder, and the library's one folder source has as label the absolute normalised path of the temporary folder, although that folder lies under no music root, because UD-19.m2 keeps rekord-api's rule of no allowed root (BR-LIB-16; ScanService.java:164-180).
- [ ] #3 Given a DJ When the DJ calls startScan with the folder "   ", with a folder that does not exist, with the path of a file and with a path holding a NUL character Then each answers 400 FOLDER_NOT_FOUND with the messages "Give a folder to scan.", "No folder there. Scanning reads this machine's own disks." (both the missing folder and the file) and "That is not a valid folder path." (BR-LIB-16).
- [ ] #4 Given a DJ who owns library B and has no active library, a library of another DJ of the same business, a library of another business and the library_id 999999 that no library has When the DJ calls startScan with a valid folder, first without library_id and then with B, the other DJ's library, the other business's library and 999999 in that order Then the answers are 400 NO_LIBRARY_SELECTED with the message "Select a library first.", 202 with a scan of B, 404 NO_LIBRARY, and twice 422 VALIDATION_FAILED with the errors item field "library_id" and code "INVALID_VALUE" (BR-LIB-07; deviation UD-19.i2; rekord-api answers 404 NO_LIBRARY to the last two).
- [ ] #5 Given a scan of library A in state scanning When the DJ calls startScan for A with a folder that does not exist, then for A with a valid folder, then for the DJ's library B Then the answers are 400 FOLDER_NOT_FOUND, 409 SCAN_IN_PROGRESS with the message "A scan of this library is already running.", and 202, so checks run library, folder, then running scan, and scans of different libraries run side by side (BR-LIB-17).
- [ ] #6 Given a finished scan of a folder with 3 files When one file is rewritten with a new title and the DJ scans again without force Then the report has from_cache 2, the rewritten file's new title is stored, and a third scan with force true has from_cache 0 (BR-LIB-19).
- [ ] #7 Given a finished scan of a folder with a.mp3 and b.mp3, and an XML source of the same library that also claims b.mp3's path When both files are deleted from disk and the DJ scans the folder again Then the library still has one folder source, a.mp3's tracks row is gone, and b.mp3's row stays because the XML source claims it (BR-LIB-13, BR-LIB-11).
- [ ] #8 Given a folder with one valid mp3 and a file broken.mp3 whose bytes are not audio When the DJ scans it Then state ends done, the library holds 2 tracks, broken.mp3's track is named from its file name with tag_source "filename", and errors holds one entry whose file is broken.mp3's path (BR-LIB-20).
- [ ] #9 Given a folder of 60 .mp3 files whose bytes are not audio When the DJ calls startScan and polls getScanStatus until the state is done Then found is never above 60 and never decreases between polls, and is 60 on every poll whose parsed is above 0 and on the final done poll; parsed never decreases and ends at 60; and each poll's errors list holds 0, 25, 50 or 60 entries, because errors are published after every 25 parsed files and at the end (BR-LIB-20; ScanService.java:252-255).
- [ ] #10 Given a scan whose track write fails with an exception carrying the message "disk full" (the test makes the write throw) When the DJ polls getScanStatus Then state is error and message is "disk full", and a new startScan of the same library answers 202 (BR-LIB-24).
- [ ] #11 Given a folder holding 2001 m4p files and 1 mp3 When the DJ scans it Then the report has skipped_drm 2001 and skipped_drm_files holds the first 2000 paths in walk order (BR-LIB-24; ScanService.java:50).
- [ ] #12 Given a DJ When the DJ calls startScan with the body {} Then the answer is 422 VALIDATION_FAILED in the error envelope, because folder is required.
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
