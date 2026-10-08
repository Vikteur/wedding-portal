---
id: TASK-23.3
title: 'P3-E03-T03 Build getScanStatus: the poll of the active library''s scan'
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-23.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:127-146'
  - 'rekord-api/src/main/java/app/rekord/scanner/ScanService.java:129-138'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryMapper.java:135-169'
  - 'rekord-contract/paths/dj.yaml:420-438'
  - 'rekord-contract/components/library.yaml:245-299'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:141'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-23
priority: high
type: feature
ordinal: 30303
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to watch how far the scan of my library has come so that I know when I can start matching.

The status is per library and read for the caller's active library, so one DJ never sees another DJ's folder path or errors; with no active library the poll answers idle, never an error, because it is polled (BR-LIB-25, LibraryResource.java:127-146). The status lives in process memory as in rekord-api: a restart forgets it, and there is no cancel and no timeout.

- Builds: `getScanStatus`
- Covers: BR-LIB-25
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E03-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ with no active library When the DJ calls getScanStatus Then the answer is 200 with state idle, errors empty and library_id null.
- [ ] #2 Given a DJ whose active library L has no scan since wedding-portal started When the DJ calls getScanStatus Then the answer is 200 with state idle, library_id L, found, parsed, from_cache and skipped_drm 0, errors empty and scanned null.
- [ ] #3 Given DJ A scanning library LA from folder FA and DJ B of the same business whose active library is LB When DJ B calls getScanStatus Then the answer is state idle for LB and holds neither FA nor any error of LA's scan (BR-LIB-25).
- [ ] #4 Given a DJ who scanned library A to the end and then selected library B When the DJ calls getScanStatus Then the answer describes B, not A.
- [ ] #5 Given a scan of the active library in state done When the DJ calls getScanStatus Then scanned holds folder, track_count, from_cache, skipped_drm, skipped_drm_files, scan_ms and scanned_at in UTC, and folder, found and parsed describe the same scan.
- [ ] #6 Given a library whose scan reached done When wedding-portal restarts and the DJ calls getScanStatus Then the answer is state idle for that library (BR-LIB-25).
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
