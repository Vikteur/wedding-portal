---
id: TASK-31.4
title: >-
  P4-E02-T04 Schema changes readable by the previous image, proven in CI on each
  migration pull request
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
  - stop-db-migration
milestone: m-4
dependencies:
  - TASK-31.3
references:
  - 'rekord-api/deploy/deploy.sh:66-70'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-31
priority: high
type: task
ordinal: 40204
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want every schema change readable by the previous image so that a rollback of the image never meets a schema it cannot use.

BR-OPS-12: a rollback never reverts the schema, so every migration must stay readable by the previous image (expand and contract), a property of the migration that the release script cannot undo (rekord-api/deploy/deploy.sh:66-70). Migrations are STOP items (database migration), and the release settings are as well. wedding-portal is released once (UD-13.d), so the previous image is, for every change, the image of the commit of main the change builds on; the check therefore runs in CI on each pull request that adds a migration, not against a release.

- STOP (human approval in the pull request): production-config, db-migration
- Covers: BR-OPS-12

Plan item `P4-E02-T04` (technical,P4,stop-production-config,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose stack started in CI with the image built from the pull request, which applied its new migration, When the rollback path of P4-E02-T03 restarts the image of the base commit of the pull request on main Then flyway_schema_history keeps the new version, no migration is reverted, and that image answers GET /api/health with 200.
- [ ] #2 Given a pull request that adds a versioned migration When CI runs Then it starts the image of the base commit of the pull request on main (tagged by its full commit SHA, P4-E02-T01) against a database migrated to the new version by the image built from the pull request in that run, Hibernate validate passes and GET /api/health answers 200, and otherwise, a missing base-commit image included, the run is red.
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
