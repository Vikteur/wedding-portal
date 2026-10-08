---
id: TASK-14.1
title: P1-E08-T01 Create the tasks table with soft delete and the board indexes
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
  - stop-db-migration
milestone: m-1
dependencies:
  - TASK-10.1
references:
  - 'rekord-api/src/main/resources/db/migration/V7__planning.sql:40-76'
  - rekord-api/src/main/java/app/rekord/domain/Task.java
  - 'docs/rewrite/STATUS.md:282-284'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-14
priority: high
type: task
ordinal: 10801
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the tasks table and the Task entity so that the task operations of P1-E08 have their storage with the database backstops of rekord-api.

Creates the table tasks as the oracle defines it (V7__planning.sql:40-76) in a new Flyway migration of wedding-portal: id uuid, org_id not null with on delete cascade, wedding_id nullable with on delete cascade (null for a task of the business), title not null, detail, status not null default OPEN checked against OPEN, IN_PROGRESS, BLOCKED, DONE and CANCELLED, priority smallint not null default 2 checked between 1 and 3, category checked against VENDOR, MUSIC, ADMIN, TIMELINE and OTHER, due_date, assignee_user_id and assignee_team_id with on delete set null, visible_to_couple not null default false, completed_at, completed_by and created_by with on delete set null, created_at, updated_at and deleted_at, and the partial indexes ix_tasks_board, ix_tasks_wedding and ix_tasks_assignee. Adds the Task entity in the persistence adapter with the fields of Task.java. category and assignee_team_id are kept although no phase-1 operation writes them, so the schema matches the oracle.

- STOP (human approval in the pull request): db-migration

Plan item `P1-E08-T01` (technical,P1,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the migrations of P1-E04-T01 When wedding-portal starts on an empty database Then Flyway applies the tasks migration, tasks has the columns, defaults, checks, foreign keys and the three partial indexes of V7__planning.sql:40-76, and Hibernate validation of Task passes.
- [ ] #2 Given a business When the repository test inserts a task with priority 4, a task with status FINISHED, a task with category PARTY and a task without title Then each insert fails, on the priority check, the status check, the category check and the not-null constraint in turn.
- [ ] #3 Given a business When the repository test inserts a task with only id, org_id and title Then the row holds wedding_id null, status OPEN, priority 2, visible_to_couple false and deleted_at null.
- [ ] #4 Given a wedding with 2 tasks, a member assigned to one of them, and a task of the business with no wedding assigned to the same member When the test deletes the wedding row and then the member's account row Then both tasks of the wedding are gone (on delete cascade), and the task without a wedding keeps its row with assignee_user_id null (on delete set null).
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
