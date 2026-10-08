---
id: TASK-23
title: 'P3-E03 Library ingestion: folder scan and rekordbox XML import'
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-22
references:
  - rekord-api/src/main/java/app/rekord/scanner/ScanService.java
  - 'rekord-api/src/main/java/app/rekord/scanner/TagReader.java:68-76'
  - rekord-api/src/main/java/app/rekord/library/RekordboxImport.java
  - rekord-api/src/main/java/app/rekord/library/LibraryService.java
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:486-512'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to scan a music folder and import my rekordbox collection into the active library so that the matcher knows every track I own.

Goal: tracks enter a library. Scope: the operations startScan, getScanStatus and importRekordboxXml; track ids as the first 12 hex characters of SHA-1 over the UTF-8 path (BR-LIB-12); the walk, change detection, tags and the report (BR-LIB-16 to BR-LIB-25); the XML parser with DTDs and external entities off and the location rules (BR-LIB-27 to BR-LIB-32); the upload limit that answers an envelope-less 413 above 10 MiB (RISK-41). startScan keeps scanning any folder the machine running wedding-portal can read, following symlinks, with no allowed music root, and importRekordboxXml keeps checking whether each named path exists: the user's choice, with RISK-08 and PIN-13-0487 accepted as risks (UD-19.m2). Out of scope: matching (P3-E04).

Plan item `P3-E03` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library and a folder holding 2 mp3 files and 1 m4p file When the DJ calls startScan for that folder and polls getScanStatus until it reports the end Then the library holds 2 tracks, each with the id formed from the first 12 hex characters of SHA-1 over its UTF-8 path, and the report counts 1 DRM file.
- [ ] #2 Given a rekordbox XML collection export of 11 MiB When the DJ calls importRekordboxXml Then the import succeeds, and a body over 256 MiB answers 413 FILE_TOO_LARGE with the error envelope (fixed defect RISK-41).
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
