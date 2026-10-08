---
id: TASK-4.2
title: >-
  P0-E04-T02 Testcontainers PostgreSQL per CI run with a test per migration and
  a real-row rollback
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 06:02'
labels:
  - technical
  - P0
  - stop-db-migration
milestone: m-0
dependencies:
  - TASK-4.1
  - TASK-3
references:
  - 'rekord-api/src/main/resources/application.properties:8'
  - 'rekord-api/src/main/resources/application.properties:71'
  - 'rekord-api/Dockerfile:31-37'
  - 'docs/rewrite/architecture-conventions.md:433-454'
  - 'rekord-api/.github/workflows/deploy.yml:121-123'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-4
priority: high
type: task
ordinal: 402
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want every database test on a fresh Testcontainers PostgreSQL with one test per migration and a rollback proven on real rows so that a schema or transaction fault turns CI red.

UD-7: every test that needs a database uses Testcontainers; rekord-api gets a fresh Dev Services PostgreSQL per run (%test reuse false; rekord-api/src/main/resources/application.properties:8, :71) and the CI run of the pushed commit decides green, while every agent may also run the tests locally (UD-21.b; .claude/CLAUDE.md P5). PIN-17-0435 asks one Testcontainers test per migration as the mechanical half of the migration STOP. PIN-AC-0452 asks the rollback test with real rows, which also proves bean discovery in a library module (§2.2); P0-E03-T02 proved the boundary without a database.

- STOP (human approval in the pull request): db-migration
- Covers: UD-7, PIN-17-0435, PIN-AC-0452, BR-OPS-29

Plan item `P0-E04-T02` (technical,P0,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CI run of the integrationTest task When a test needs a database Then it gets a postgres:17-alpine container started by Testcontainers with reuse off, so no row survives from an earlier run.
- [ ] #2 Given the versioned Flyway migrations When the integrationTest task runs Then one test per versioned migration applies the history up to and including it on an empty container and asserts that the tables, the columns with their types and nullability, and the constraints read from information_schema equal the list that test declares for that migration, and a V file without such a test fails the build.
- [ ] #3 Given a test-only use case whose first port call inserts a row into a test-only table through a persistence adapter in a library module and whose second port call throws When a @QuarkusTest calls it Then the row does not exist afterwards, and the adapter bean was discovered through its module's Jandex index.
- [ ] #4 Given one commit of wedding-portal When CI runs Then the test job runs the test and integrationTest tasks on that SHA, and the image job of P0-E01-T03 declares needs: on the test job and does not run when the test job fails.
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
