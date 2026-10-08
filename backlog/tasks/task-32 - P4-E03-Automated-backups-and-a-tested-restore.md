---
id: TASK-32
title: P4-E03 Automated backups and a tested restore
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - epic
  - P4
milestone: m-4
dependencies:
  - TASK-30
  - TASK-31
references:
  - 'docs/rewrite/repo-and-contract-decision.md:811-865'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:235-259'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
priority: high
ordinal: 40300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want the wedding data backed up automatically and restorable so that a bad release or a broken migration never loses my weddings.

Goal: recoverable data on the release server (H9, RISK-56), since a Flyway migration cannot be rolled back. Scope: automated PostgreSQL backups of the wedding-portal database, their storage outside the database container, and a restore that is exercised before the release. Target, schedule and retention are decided later by the user; they are the deployment settings BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS, set only in /opt/wedding-portal/.env on the server and without defaults, BACKUP_SCHEDULE read in UTC, and the release (P4-E04-T02) stops when any of them is unset (UD-19.n3). The criteria run against the compose stack started in CI from the release configuration, because wedding-portal is deployed only once (UD-13.d); the first scheduled backup on the new server is checked in that single release, P4-E04-T02. Out of scope: the POC data, which is archived and never imported (UD-13.c, P4-E04).

Plan item `P4-E03` (epic,P4) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose stack started in CI from the release configuration, with BACKUP_TARGET set to a directory outside the database container, BACKUP_SCHEDULE set to 0 3 * * * and BACKUP_RETENTION_DAYS set to 7 When the backup job is triggered as its schedule triggers it Then a PostgreSQL backup of the wedding-portal database is written into that directory, and restoring it into an empty database starts wedding-portal with the same rows.
- [ ] #2 Given a release that applies a new Flyway migration When the pipeline runs Then a backup taken before the migration exists before the new version starts.
- [ ] #3 Given the compose stack started in CI with BACKUP_TARGET unset When the backup job is triggered Then it exits non-zero naming BACKUP_TARGET and writes no dump (UD-19.n3).
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
