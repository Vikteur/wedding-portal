---
id: TASK-28
title: P3-E08 Per-couple rekordbox export
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-27
references:
  - 'docs/rewrite/STATUS.md:331-336'
  - 'rekord-backend/server/main.py:600-678'
  - 'rekord-backend/server/export/couple.py:1-58'
  - docs/rewrite/analysis/20-unmerged-and-python-only-work.md
  - docs/rewrite/analysis/14-dj-matching-exports.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30800
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want one rekordbox XML per wedding with a folder named after the couple and the date and one numbered playlist per chapter in the order of the night so that the whole night loads in one import.

Goal: the per-couple export of the POC, written in Java (UD-16 UX-01, UD-8). Scope: three new operations per wedding, a contract change and a STOP item: a preview summary, one rekordbox XML with a folder "names date" holding one numbered playlist per chapter in the order of the night with never-list songs filtered out, and a missing-songs list de-duplicated over all chapters; chapters, export rules, entry conversion, the folder XML and the refusals (BR-MX-37 to BR-MX-41), specified by the POC (rekord-backend/server/main.py:609, :631, :653; rekord-backend/server/export/couple.py:17-24). Out of scope: the per-playlist exports (P3-E07).

Plan item `P3-E08` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding whose song lists hold matched songs in three chapters and one never-listed song When the DJ calls the new rekordbox XML operation for that wedding Then the file holds one folder named with the couple's names and the wedding date, holding one numbered playlist per non-empty chapter in the order of the night, and the never-listed song is in no playlist.
- [ ] #2 Given the same wedding When the DJ calls the new preview and missing-songs operations Then the preview counts the matched, missing and blocked songs and the missing list holds each missing song once over all chapters.
- [ ] #3 Given the contract tag wedding-portal pins at the end of phase 3 When its DJ authoring file is read Then it holds the three per-couple export operations, merged after a recorded human approval of the contract push.
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
