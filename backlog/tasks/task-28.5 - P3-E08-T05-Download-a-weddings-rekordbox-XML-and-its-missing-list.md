---
id: TASK-28.5
title: P3-E08-T05 Download a wedding's rekordbox XML and its missing list
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-28.3
  - TASK-28.4
references:
  - 'rekord-backend/server/main.py:631-669'
  - 'rekord-backend/tests/test_couple_export.py:96-184'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:171'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-28
priority: high
type: feature
ordinal: 30805
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to download one rekordbox XML for a whole wedding and one list of the songs I still lack so that I load the night in one import and know what to buy.

The operations exportCoupleRekordboxXml and exportCoupleMissing of P3-E08-T01, built in Java from the POC (rekord-backend/server/main.py:631-669). Both assemble the chapters of P3-E08-T02 and check the wedding and the library as getCoupleExportSummary does (P3-E08-T04). The XML is the folder XML of P3-E08-T03; the missing list is the missing-list builder of P3-E07-T01 with the folder label as the playlist name. Download names follow rekord-api's rule of P3-E07-T02 applied to the folder label (UD-16.UX-15), so accents survive; the POC sent only an ASCII filename (BR-MX-35). Both bodies go straight into the response with no temporary file (P3-E07-T02). This ticket builds exportCoupleRekordboxXml and exportCoupleMissing; builds stays empty because the operations are added to the contract by P3-E08-T01 and are not catalog operations.

- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-41, UX-15, UD-16.UX-15, PIN-20-0253
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: besides the 409 NO_LIBRARY of an active library without files, requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId (LibraryRepository.java:174-188) and the re-read in require (LibraryRepository.java:215-222); no criterion
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the operation writes no rows
- Error code `VALIDATION_FAILED` without a criterion here: a wedding id that is not a UUID answers 422 VALIDATION_FAILED through P0-E05 (UD-12)

Plan item `P3-E08-T05` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the wedding of P3-E08-T04's first criterion When the DJ calls exportCoupleRekordboxXml Then the answer is 200 with Content-Type application/xml; charset=utf-8 and the folder XML of P3-E08-T03 for its two playlists, which the collection import of P3-E03 reads back (test_couple_export.py:96-116; main.py:631-646).
- [ ] #2 Given the same wedding When the DJ calls exportCoupleRekordboxXml Then the Content-Disposition is attachment; filename="Emma & Julian 2026-09-12.xml"; filename*=UTF-8''Emma%20%26%20Julian%202026-09-12.xml, and for the names "Emma & Julián" the plain name reads "Emma & Juli_n 2026-09-12.xml" (UD-16.UX-15; the POC writes an ASCII-only name, main.py:643-645).
- [ ] #3 Given three weddings: one whose entries are all absent from the active library, one whose entries are all on its never list, and one whose song lists hold no entry with a title or free_text When the DJ calls exportCoupleRekordboxXml for each Then each answer is 400 NOTHING_MATCHED with the message "None of their songs are in this library yet — export the missing list instead, then re-import your collection once you have them." (main.py:635-641).
- [ ] #4 Given a wedding with ("Ghost Act", "A Song Nobody Owns") in couple_top20 and in friends_top20 and absent from the active library "Studio PC" When the DJ calls exportCoupleMissing Then the answer is 200 with Content-Type text/plain; charset=utf-8 and the missing list of P3-E07-T01 whose playlist line reads "# Playlist: " and the folder label, whose count line names Studio PC, and which holds "Ghost Act - A Song Nobody Owns" once (test_couple_export.py:157-172; main.py:650-669).
- [ ] #5 Given the same wedding When the DJ calls exportCoupleMissing Then the Content-Disposition is attachment; filename="Emma & Julian 2026-09-12 - missing.txt"; filename*=UTF-8''Emma%20%26%20Julian%202026-09-12%20-%20missing.txt (UD-16.UX-15).
- [ ] #6 Given three weddings: one whose entries are all auto-selected in the active library, one whose entries are all on its never list, and one whose song lists hold no entry with a title or free_text When the DJ calls exportCoupleMissing for each Then each answer is 400 NOTHING_MISSING with the message "You already have every song they asked for." (main.py:654-658).
- [ ] #7 Given an unknown wedding id, a DJ with no active library, an active library with no files and an active library being scanned When the DJ calls exportCoupleRekordboxXml and exportCoupleMissing for each Then each answers as getCoupleExportSummary does in P3-E08-T04: 404 NO_WEDDING, 400 NO_LIBRARY_SELECTED, 409 NO_LIBRARY and 409 SCAN_IN_PROGRESS (test_couple_export.py:174-184).
- [ ] #8 Given a DJ who downloads either file When wedding-portal answers Then the body is written straight into the response and no file is created under the temporary directory of the JVM.
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
