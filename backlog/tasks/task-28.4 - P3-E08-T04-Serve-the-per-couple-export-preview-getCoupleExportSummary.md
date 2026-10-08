---
id: TASK-28.4
title: 'P3-E08-T04 Serve the per-couple export preview: getCoupleExportSummary'
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-28.2
  - TASK-21.2
references:
  - 'rekord-backend/server/main.py:596-628'
  - 'rekord-backend/tests/test_couple_export.py:96-154'
  - 'rekord-backend/tests/test_couple_export.py:174-184'
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:140'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:72-90'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-28
priority: high
type: feature
ordinal: 30804
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see how many songs of a wedding are matched, missing and blocked, chapter by chapter, before I download anything so that I know whether the library is ready for the night.

The operation getCoupleExportSummary of P3-E08-T01 answers the counts of the chapters of P3-E08-T02 for one wedding (rekord-backend/server/main.py:609-628). The wedding is found by the assignment rule of getDjWedding (P3-E01-T02), so the POC's 404 NO_COUPLE becomes rekord-api's 404 NO_WEDDING. The library checks are those of matchTracks (P3-E04-T06), so the POC's 409 NO_LIBRARY for a missing library becomes 400 NO_LIBRARY_SELECTED, and 409 NO_LIBRARY stays for an active library without files (BR-MX-41). It writes nothing. This ticket builds getCoupleExportSummary; builds stays empty because the operation is added to the contract by P3-E08-T01 and is not one of the catalog operations.

- STOP (human approval in the pull request): auth-access
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: besides the 409 NO_LIBRARY of an active library without files, requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId (LibraryRepository.java:174-188) and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the operation writes no rows
- Error code `VALIDATION_FAILED` without a criterion here: a wedding id that is not a UUID answers 422 VALIDATION_FAILED through P0-E05 (UD-12); asserted by the seventh criterion

Plan item `P3-E08-T04` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding of Emma & Julian on 2026-09-12 whose opening_dance and couple_top20 lists each hold an entry auto-selected in the active library "Studio PC", and a DJ assigned to it When the DJ calls getCoupleExportSummary Then the answer is 200 with wedding (its id, couple_display_name and wedding_date), folder "Emma & Julian 2026-09-12", library "Studio PC", playlists [{name "01 Opening dance", tracks 1}, {name "02 Their top 20", tracks 1}], matched 2, missing 0 and blocked 0 (test_couple_export.py:96-116).
- [ ] #2 Given a wedding whose only entry is on its never list and in the library When the DJ calls getCoupleExportSummary Then the answer is 200 with playlists [], matched 0, missing 0 and blocked 1 (test_couple_export.py:134-154).
- [ ] #3 Given a wedding whose song lists hold no entries When the DJ calls getCoupleExportSummary Then the answer is 200 with playlists [] and matched, missing and blocked 0.
- [ ] #4 Given a wedding of another business, a wedding of the same business the DJ fills no slot of, a soft-deleted wedding and an unknown id When the DJ calls getCoupleExportSummary for each Then each answers 404 NO_WEDDING with the message "There is no such wedding." of getDjWedding (CouplesResource.java:140; the POC answers 404 NO_COUPLE, main.py:596-599).
- [ ] #5 Given an assigned wedding and a DJ with no active library When the DJ calls getCoupleExportSummary Then the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first." (the POC answers 409 NO_LIBRARY, main.py:600-605).
- [ ] #6 Given an assigned wedding and an active library with no files, and separately an active library whose scan is in state scanning When the DJ calls getCoupleExportSummary Then the answers are 409 NO_LIBRARY and 409 SCAN_IN_PROGRESS with the messages matchTracks gives (P3-E04-T06; MatchingResource.java:72-90).
- [ ] #7 Given a wedding id that is not a UUID When the DJ calls getCoupleExportSummary Then the answer is 422 VALIDATION_FAILED.
- [ ] #8 Given an assigned wedding When the DJ calls getCoupleExportSummary twice with no change in between Then both answers are equal, and no row of the wedding, its song lists, the library or its preferences changes.
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
