---
id: TASK-27.3
title: 'P3-E07-T03 Build exportMissing: the shopping list of songs the library lacks'
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-27.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:104-130'
  - 'rekord-api/src/test/java/app/rekord/library/LibraryExportTest.java:151-166'
  - 'rekord-contract/components/library.yaml:641-671'
  - 'rekord-contract/paths/dj.yaml:617-645'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-27
priority: high
type: feature
ordinal: 30703
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to download the requested songs my library does not hold as a plain list split into not found and turned down so that I know which songs to buy and which versions to recheck.

The operation POST /api/export/missing takes name, tracks (at least one; each a title with an optional artist, default '', and had_candidates, default false) and the optional wedding_id (library.yaml:641-671). It needs an active library only for the library's name in the header, never checks the tracks against the library, and answers the missing-list body of P3-E07-T01 as the download "<name> - missing.txt" (ExportsResource.java:104-130). Whether a track had candidates is decided by the caller (BR-MX-32). The never list behind wedding_id is P3-E07-T05.

- Builds: `exportMissing`
- Covers: BR-MX-32, PIN-14-0505
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `NO_TRACKS` without a criterion here: answered 400 when every requested track is on the never list of the given wedding; specified by P3-E07-T05, because an empty tracks list answers 422 VALIDATION_FAILED first
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the export writes no rows, so no race of it reaches this code

Plan item `P3-E07-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and an active library When a characterization test calls exportMissing with no Accept header and one missing track and records the status, the Content-Type (media type and charset) and the Content-Disposition Then the same test passes against wedding-portal, so the Content-Type is the one rekord-api sends, with the media type text/plain, the only one the pinned contract declares for the 200 response of exportMissing (PIN-14-0505; paths/dj.yaml:640).
- [ ] #2 Given a DJ whose active library is named "Studio PC" When the DJ calls exportMissing with name "Emma & Julian" and the tracks ("Avicii", "Levels", had_candidates false) and ("Prince", "Purple Rain", had_candidates true) Then the answer is 200 with the missing-list body of P3-E07-T01 for that input, "Avicii - Levels" before "Prince - Purple Rain" (LibraryExportTest.java:151-166).
- [ ] #3 Given the same request When wedding-portal answers Then the Content-Disposition is attachment; filename="Emma & Julian - missing.txt"; filename*=UTF-8''Emma%20%26%20Julian%20-%20missing.txt, built from the name rule and the header rule of P3-E07-T02 (BR-MX-34; ExportsResource.java:127-129).
- [ ] #4 Given a track sent with only a title When the DJ calls exportMissing Then the track is listed under not found with its title alone, because artist defaults to '' and had_candidates to false (library.yaml:641-654, ExportsResource.java:114-120).
- [ ] #5 Given a track that the active library holds, sent with had_candidates false When the DJ calls exportMissing Then it is still listed under not found: the server takes had_candidates as sent and never looks the track up (BR-MX-32; ExportsResource.java:104-130).
- [ ] #6 Given a DJ with an active library When the DJ calls exportMissing with tracks [], then with a track without title, then without name Then each answer is 422 VALIDATION_FAILED from the contract's constraints, so the NO_TRACKS refusal at ExportsResource.java:108 is never reached (PIN-14-0507).
- [ ] #7 Given a DJ with no active library When the DJ calls exportMissing with a valid body Then the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
- [ ] #8 Given a DJ who downloads a missing list When wedding-portal answers exportMissing Then the body is written straight into the response and no file is created under the temporary directory of the JVM (fixed defect, analysis 14-dj-matching-exports.md:427: export temp files hold paths and names for up to 1 h; rekord-api writes each body to java.io.tmpdir/rekord-exports and sweeps it after 1 h, ExportsResource.java:49-50, :173-185, :210-240).
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
