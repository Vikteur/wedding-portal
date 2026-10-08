---
id: TASK-12.1
title: >-
  P1-E06-T01 Create the song_changes table and the writer for planner link
  activity rows
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - technical
  - P1
  - stop-db-migration
milestone: m-1
dependencies:
  - TASK-10.1
references:
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:133-148'
  - rekord-api/src/main/java/app/rekord/domain/SongChange.java
  - 'rekord-backend/server/couples_api.py:184-204'
  - 'rekord-backend/server/couples.py:582-597'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:108-123'
  - 'docs/rewrite/STATUS.md:337'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: task
ordinal: 10601
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the song_changes table and one writer for planner link activity so that rotate, revoke and reissue each leave a row the DJ's feed reads later.

Creates the song_changes table as the oracle defines it (V6__music.sql:133-148) in a new Flyway migration of wedding-portal: id bigserial, org_id and wedding_id with on delete cascade, source_kind checked against couple, friend and dj, action checked against added, updated, removed, reordered and details, kind and uid nullable text, summary not null, at timestamptz default now(), and the index ix_song_changes_wedding on (wedding_id, id desc). Adds the SongChange entity (SongChange.java) and one writer that the link operations of P1-E06-T03 to P1-E06-T05 call inside their own transaction, so a refused or failed call leaves no row. The row shape of a planner link action follows the Python POC (couples_api.py:184-204): source_kind dj, action details, kind and uid null, an English summary. Deviation: phase 1 creates this table, not phase 2, because UD-16.UX-03 makes the planner's link actions write rows; the couple and friend rows of P2-E05-T02 use the same table and the feed of P3-E01-T04 reads it.

- STOP (human approval in the pull request): db-migration

Plan item `P1-E06-T01` (technical,P1,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the migrations of P1-E01 to P1-E04 When wedding-portal starts on an empty database Then Flyway applies the song_changes migration, the table has the columns, checks and index ix_song_changes_wedding of V6__music.sql:133-148, and Hibernate validation of SongChange passes.
- [ ] #2 Given a wedding When the repository test inserts a row with source_kind planner, a row with action rotated, and a row without summary Then all three inserts fail on the source_kind check, the action check and the not-null constraint in turn.
- [ ] #3 Given a wedding with 2 song_changes rows When the wedding row is deleted in the test Then both rows are gone (on delete cascade).
- [ ] #4 Given the writer and a wedding of a business When a test calls the writer with summary "rotated the couple link" inside a transaction that then rolls back, and once inside a transaction that commits Then exactly one row exists, with the business's org_id, the wedding_id, source_kind dj, action details, kind null, uid null, the summary and at set by the database.
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
