---
id: TASK-28.1
title: P3-E08-T01 Contract change adding the three per-couple export operations
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - technical
  - P3
  - stop-contract-push
milestone: m-3
dependencies:
  - TASK-27.5
  - TASK-2
references:
  - 'rekord-contract/paths/dj.yaml:578-668'
  - 'rekord-contract/openapi.yaml:195-241'
  - 'rekord-contract/components/common.yaml:36-108'
  - 'rekord-backend/server/main.py:609-669'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:45'
  - 'docs/rewrite/STATUS.md:330-336'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-28
priority: high
type: task
ordinal: 30801
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want the per-couple export operations and their two refusal codes in the contract hub before any code uses them so that wedding-portal and the DJ app regenerate from one agreed shape.

UD-16.UX-01 keeps the per-couple export of the Python POC and has it written in Java (UD-8). rekord-api has no such operation (UX-01: Exports.java:127-166 writes a single playlist), so the contract gains three operations under the DJ's wedding paths: a preview summary, the rekordbox XML of the whole wedding and the de-duplicated missing list, plus the refusal codes NOTHING_MATCHED and NOTHING_MISSING of the POC (rekord-backend/server/main.py:609-669). The operation names and paths below are the names this plan gives them; the POC used /api/couples/{couple_id}/export/... This is a contract push: it merges only after a recorded human approval.

- STOP (human approval in the pull request): contract-push
- Covers: UD-16.UX-01, UX-01

Plan item `P3-E08-T01` (technical,P3,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-contract/paths/dj.yaml and openapi.yaml When the change is read Then they hold GET /dj/weddings/{weddingId}/export/summary with operationId getCoupleExportSummary, GET /dj/weddings/{weddingId}/export/rekordbox.xml with operationId exportCoupleRekordboxXml and GET /dj/weddings/{weddingId}/export/missing.txt with operationId exportCoupleMissing, each tagged exports, with the WeddingId path parameter, the security of the other DJ operations and the common Error response as default.
- [ ] #2 Given the operation getCoupleExportSummary When it is read Then its 200 answer is application/json with the new schema CoupleExportSummary in components/library.yaml, which requires wedding (id as Uuid, couple_display_name as string, wedding_date as IsoDate), folder (string), library (string), playlists (an array of objects requiring name as string and tracks as an integer of at least 0), and matched, missing and blocked (integers of at least 0).
- [ ] #3 Given the operations exportCoupleRekordboxXml and exportCoupleMissing When they are read Then their 200 answers are application/xml and text/plain, each a string of format binary with a Content-Disposition header, in the shape of exportPlaylist and exportMissing (rekord-contract/paths/dj.yaml:578-645), and each operation's description states that the body is sent with charset=utf-8, as the POC sends (main.py:646-648, :665-667).
- [ ] #4 Given ErrorCode in components/common.yaml When the change is read Then it holds the new values NOTHING_MATCHED and NOTHING_MISSING, and no existing value is removed or renamed.
- [ ] #5 Given the descriptions of the three operations When they are read Then they name the answers 400 NO_LIBRARY_SELECTED, 404 NO_WEDDING, 409 NO_LIBRARY, 409 SCAN_IN_PROGRESS and 422 VALIDATION_FAILED for all three, 400 NOTHING_MATCHED for exportCoupleRekordboxXml and 400 NOTHING_MISSING for exportCoupleMissing, and state that the never list of the wedding is applied and that the summary writes nothing.
- [ ] #6 Given the pull request with the change When the contract CI of P0-E02 runs Then lint passes, the breaking-change job reports no removed or renamed operation, property or enum value and no newly required property of an existing schema, and the merge commit gets a tag that wedding-portal then pins (UD-16.UX-01).
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
