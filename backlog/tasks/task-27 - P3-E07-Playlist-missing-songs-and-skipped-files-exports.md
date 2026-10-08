---
id: TASK-27
title: 'P3-E07 Playlist, missing-songs and skipped-files exports'
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-26
  - TASK-25
references:
  - 'docs/rewrite/STATUS.md:345'
  - rekord-api/src/main/java/app/rekord/library/ExportsResource.java
  - rekord-api/src/main/java/app/rekord/library/Exports.java
  - rekord-api/src/main/java/app/rekord/library/Blocklist.java
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:504-516'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30700
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to download the matched set as an m3u8 or rekordbox XML file, plus the missing songs and the skipped files, without the songs on the couple's never list so that I load the night into my DJ software.

Goal: the exports of rekord-api. Scope: the operations exportPlaylist, exportMissing and exportSkipped; the m3u8 and rekordbox XML formats, the location and kind rules, the skipped list and the download names (BR-MX-24 to BR-MX-36, BR-LIB-26); the never list applied at export (BR-LIB-47, BR-LIB-48); filenames as rekord-api writes them (UX-15); the untagged file the never list misses (RISK-28). exportPlaylist refuses a track id the active library does not hold with 422 VALIDATION_FAILED naming track_ids[i], and exportPlaylist and exportMissing answer a wedding_id the caller cannot see with the 404 NO_WEDDING that getDjWedding gives for it (deviation UD-19.m7, RISK-09). Out of scope: the per-couple export (P3-E08).

Plan item `P3-E07` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given matched tracks and a wedding the DJ is booked for whose never list holds one of their artist and title pairs When the DJ calls exportPlaylist with format m3u8 and that wedding_id Then the file holds every requested track except the never-listed one, in request order.
- [ ] #2 Given an untagged library file named after a never-listed title When the DJ calls exportPlaylist with that track and the wedding_id Then the file leaves it out (fixed defect RISK-28; rekord-api exports it).
- [ ] #3 Given an active library holding f1 and a last scan that stepped over a DRM file When the DJ calls exportPlaylist with name "Emma & Julián" and format m3u8, exportMissing with name "Emma & Julián", and exportSkipped Then the Content-Disposition headers are attachment; filename="Emma & Juli_n.m3u8"; filename*=UTF-8''Emma%20%26%20Juli%C3%A1n.m3u8, attachment; filename="Emma & Juli_n - missing.txt"; filename*=UTF-8''Emma%20%26%20Juli%C3%A1n%20-%20missing.txt and attachment; filename="skipped.txt"; filename*=UTF-8''skipped.txt (UD-16.UX-15).
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
