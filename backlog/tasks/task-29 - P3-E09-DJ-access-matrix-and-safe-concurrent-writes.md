---
id: TASK-29
title: P3-E09 DJ access matrix and safe concurrent writes
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-28
references:
  - 'docs/rewrite/STATUS.md:296-306'
  - 'docs/rewrite/business-analysis.md:1463'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:435-436'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30900
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want every DJ operation to admit exactly the sessions the access matrix names and to answer two simultaneous writes without a server error so that the DJ side is safe to open to the whole business.

Goal: one table-driven access test over every operation of phase 3, with the admin rule of UD-14.b3 (an admin may call every DJ operation on every wedding of the business without a team-slot assignment), and the concurrency defects of RISK-48 fixed: two simultaneous scans of one library, an index build that overwrites a later invalidation, and unique-index races, settled by the race policy of P0-E05-T05: a library or playlist write that locks the libraries row, a settings write by INSERT … ON CONFLICT and a scan start under an in-process lock give both calls the answers of a sequential pair, while the loser of a library-name race of createLibrary or renameLibrary, which only the unique index catches, answers 409 DUPLICATE_NAME with the message of the name check (deviation UD-19.i5, RISK-26). Scope: P3-E09-T01 (access matrix) and P3-E09-T02 (races). Out of scope: the behaviour of each operation, which its own ticket in P3-E01 to P3-E08 settles.

Plan item `P3-E09` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the access matrix test of P3-E09-T01 When it runs in CI Then every operation built in P3-E01 to P3-E08 has a row for no session, a couple portal session, a DJ, a planner and an admin, and the test fails when an operation of the pinned contract file paths/dj.yaml or of P3-E08-T01 has no row.
- [ ] #2 Given two simultaneous calls of each race listed in P3-E09-T02 When the race test runs in CI Then no call answers 500, the losers of the name races of criteria 1 and 2 of P3-E09-T02 answer 409 DUPLICATE_NAME (deviation UD-19.i5; rekord-api answers 500 UNKNOWN), and the stored rows are the ones that ticket names.
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
