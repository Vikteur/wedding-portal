---
id: TASK-32.1
title: >-
  P4-E03-T01 Automated PostgreSQL backups outside the database container and
  before a schema change
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - user-story
  - P4
  - stop-production-config
  - stop-db-migration
milestone: m-4
dependencies:
  - TASK-30
  - TASK-31.3
references:
  - 'rekord-backend/docs/deploy.md:204-215'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:247'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-32
priority: high
type: feature
ordinal: 40301
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want the wedding data backed up automatically, and again before a schema change, so that a bad release never loses my weddings.

H9 and RISK-56: neither POC stack has automated backups; the Python docs give a manual procedure and recommend a weekly cron (rekord-backend/docs/deploy.md:204-215), and rekord-api's PostgreSQL has no dump job (16 R-09). A rollback cannot undo a migration (BR-OPS-12). UD-19.n3: target, schedule and retention are decided later by the user, so the job reads them from three deployment settings in the settings file of wedding-portal on the server, with no default in git or in the job, read from /opt/wedding-portal/.env: BACKUP_TARGET, the directory on the server the dumps are written to (storage outside the server is mounted there); BACKUP_SCHEDULE, a five-field cron expression read in UTC; BACKUP_RETENTION_DAYS, a whole number of days after which a dump of this job is removed. The release stops when any of them is unset (P4-E02-T03, P4-E04-T02). A set value the job cannot use is refused like an unset one, by the job and by the release script: a BACKUP_SCHEDULE that is not five cron fields, a BACKUP_RETENTION_DAYS that is not a whole number of at least 1, and a BACKUP_TARGET that is not an existing directory the job can write to. The dump the release script takes before a migration is written to the same directory and follows the same retention rule. On the first release (UD-13.d) the database holds no flyway_schema_history yet, so there is nothing to dump. Backups touch the release settings and guard migrations, so the ticket is a STOP item for both. Its criteria run in CI; the first scheduled run on the new server is checked in the release, P4-E04-T02.

- STOP (human approval in the pull request): production-config, db-migration
- Covers: H9, RISK-56, UD-19.n3

Plan item `P4-E03-T01` (user-story,P4,stop-production-config,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose stack started in CI from the release configuration in git, with BACKUP_TARGET set to a directory on the runner outside the database container and its volume, BACKUP_SCHEDULE set to 0 3 * * * and BACKUP_RETENTION_DAYS set to 7 When the backup job is triggered as its schedule triggers it Then pg_dump writes a dump of the wedding-portal database into the BACKUP_TARGET directory, and the job exits non-zero when the dump fails.
- [ ] #2 Given the compose stack started in CI from the release configuration in git, with BACKUP_TARGET set to a directory on the runner outside the database container and its volume, BACKUP_SCHEDULE set to 0 3 * * * and BACKUP_RETENTION_DAYS set to 7 When the schedule installed by the release configuration is listed Then the backup job is scheduled with exactly the cron expression 0 3 * * *, read in UTC so that it runs at 03:00 UTC, and with no other schedule (UD-19.n3).
- [ ] #3 Given the compose stack started in CI with any one of BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS unset or empty When the backup job or its scheduling starts Then it exits non-zero naming the missing key, writes no dump and uses no default value (UD-19.n3).
- [ ] #4 Given the compose stack started in CI with BACKUP_SCHEDULE set to 0 3 * *, and in further runs BACKUP_RETENTION_DAYS set to 0, to -1 and to abc, every other key set to its ci/test.env value When the backup job or its scheduling starts and when the release script of P4-E02-T03 runs Then each exits non-zero naming the refused key, the job writes no dump and the release script stops before docker compose pull (UD-19.n3).
- [ ] #5 Given the compose stack started in CI with BACKUP_TARGET set to /nonexistent, and in a second run to a directory owned by root with mode 555 that the job cannot write to When the backup job starts and when the release script of P4-E02-T03 runs Then each exits non-zero naming BACKUP_TARGET, the job writes no dump and the release script stops before docker compose pull (UD-19.n3).
- [ ] #6 Given the compose stack started in CI from the release configuration in git, with BACKUP_TARGET set to a directory on the runner outside the database container and its volume, BACKUP_SCHEDULE set to 0 3 * * * and BACKUP_RETENTION_DAYS set to 7, and the BACKUP_TARGET directory holding one dump of this job written 8 days ago and one written 6 days ago When the backup job writes a new dump successfully Then the 8-day-old dump is removed, and the 6-day-old dump and the new dump remain (UD-19.n3).
- [ ] #7 Given the compose stack started in CI from the release configuration in git, with BACKUP_TARGET set to a directory on the runner outside the database container and its volume, BACKUP_SCHEDULE set to 0 3 * * * and BACKUP_RETENTION_DAYS set to 7, and the BACKUP_TARGET directory holding one dump of this job written 8 days ago When the new dump fails Then the job exits non-zero and the 8-day-old dump remains (UD-19.n3).
- [ ] #8 Given the compose file, the backup job and the release workflow in git When they are read Then BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS appear only as keys, with no value and no default (UD-19.n3).
- [ ] #9 Given a release whose image holds a versioned migration higher than the highest successful version in flyway_schema_history of the server database When the release script runs Then it takes a dump into the BACKUP_TARGET directory and checks that the file exists and is not empty before the new version starts, stops the release without starting it when the check fails, and the retention rule later removes that dump like a scheduled one once it is older than BACKUP_RETENTION_DAYS days.
- [ ] #10 Given a server database without the table flyway_schema_history, as on the first release (UD-13.d) When the release script runs Then it takes no pre-migration dump, writes a line saying so into the run log and goes on with the release.
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
