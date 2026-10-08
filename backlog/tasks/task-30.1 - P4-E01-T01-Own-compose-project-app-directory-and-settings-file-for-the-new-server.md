---
id: TASK-30.1
title: >-
  P4-E01-T01 Own compose project, app directory and settings file for the new
  server
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-production-config
milestone: m-4
dependencies: []
references:
  - 'rekord-api/deploy/docker-compose.yml:1-68'
  - 'rekord-api/deploy/deploy.sh:11-16'
  - 'rekord-api/deploy/deploy.sh:48'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:245'
  - 'rekord-api/src/main/resources/application.properties:35'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:120'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: task
ordinal: 40101
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want wedding-portal to run in a compose project, directory and settings file of its own so that no other stack is removed, replaced or reconfigured by a release.

H1 and RISK-55: the POC's two stacks share one app directory, the compose project rekordmatch and one .env, and both run up -d --remove-orphans, so a first rekord-api release would remove the Python containers and start an empty database (16 R-07; rekord-api/deploy/docker-compose.yml:3; rekord-api/deploy/deploy.sh:11, :48). BR-OPS-13: containers bind loopback only. rekord-api's compose file is the oracle for health checks, the named database volume and the rotation of container output (rekord-api/deploy/docker-compose.yml:7-67). The paths are fixed here and every ticket of this phase uses them: the app directory is /opt/wedding-portal, the settings file /opt/wedding-portal/.env and the web root of P4-E01-T06 /opt/wedding-portal/web. The release configuration of the new server is a STOP item (production configuration). Its criteria run in CI; the check on the new server is part of the release, P4-E04-T02. UD-19.n2 and UD-19.n3: the host name (PUBLIC_HOST) and the backup target, schedule and retention (BACKUP_TARGET, BACKUP_SCHEDULE, BACKUP_RETENTION_DAYS) are deployment settings whose values are set only in /opt/wedding-portal/.env on the server, with no default in the release configuration. CI needs test values for them, so they live in one tracked file, ci/test.env, which only CI reads; the secret test values (the database password and the first admin's password) are generated in each CI run and never tracked. UD-19.n2 also reaches the app: wedding-portal builds every couple link as app.public-base-url followed by /g/ and the token (BR-PL-19, P1-E06-T02) and every invite accept_url as app.public-base-url followed by /invite/ and the token (P1-E02-T06), and rekord-api reads that setting from PUBLIC_BASE_URL with the default http://localhost:8080 (rekord-api/src/main/resources/application.properties:35; WeddingMapper.java:150; AccountsResource.java:120), so the release configuration derives it from PUBLIC_HOST and no release link can start with that default.

- STOP (human approval in the pull request): production-config
- Covers: H1, RISK-55, BR-OPS-13, UD-19.n2, UD-19.n3

Plan item `P4-E01-T01` (technical,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the compose file of wedding-portal in git When it is read Then its compose project name is wedding-portal, every setting reaches a container only by its key through ${KEY} interpolation, and neither the name rekordmatch nor the directory /opt/rekordmatch appears in it.
- [ ] #2 Given a probe container running on the CI runner in a compose project other than wedding-portal When CI runs up -d --remove-orphans with the compose file of wedding-portal Then the probe container keeps running, and only containers of the wedding-portal compose project are created, recreated or removed.
- [ ] #3 Given the compose file When it is read Then the app and the PostgreSQL 17 database each have a health check, the app waits for the database to be healthy, the database keeps its data in a named volume, and both containers rotate their json-file output at max-size 10m and max-file 3.
- [ ] #4 Given a pull request of wedding-portal When CI runs its settings check over the tracked files Then the check fails when any tracked file holds a secret value or when any tracked file other than ci/test.env gives PUBLIC_HOST, BACKUP_TARGET, BACKUP_SCHEDULE or BACKUP_RETENTION_DAYS a value or a default, so the compose file, the nginx site file, the backup job, the release script and the release workflow name them only as keys (UD-19.n2, UD-19.n3).
- [ ] #5 Given ci/test.env in git When it is read Then it sets PUBLIC_HOST=portal.example.com, BACKUP_TARGET=/tmp/wedding-portal-backups, BACKUP_SCHEDULE=0 3 * * * and BACKUP_RETENTION_DAYS=7, holds no secret value and no other host name, and the release script and the release workflow name ci/test.env nowhere.
- [ ] #6 Given the compose file in git When the environment of the app service is read Then it sets PUBLIC_BASE_URL, the source of app.public-base-url (rekord-api/src/main/resources/application.properties:35), to https://${PUBLIC_HOST} with no default, so no couple link or invite link of the release starts with http://localhost:8080 (UD-19.n2).
- [ ] #7 Given the compose file When its published ports are read Then every port binds 127.0.0.1, the app's as 127.0.0.1:${APP_PORT:-8080}:8080, and no service publishes a port on all interfaces (BR-OPS-13).
- [ ] #8 Given a pull request of wedding-portal When CI starts the compose file from git on the runner as compose project wedding-portal with the settings file ci/test.env and the first admin signs in and creates a test wedding Then both containers report healthy, GET http://127.0.0.1:8080/api/health answers 200 {"ok":true}, the url of each portal of that wedding from getWeddingPortal starts with https://portal.example.com/g/, and the accept_url of a createInvite for dj@example.com starts with https://portal.example.com/invite/ (UD-19.n2).
- [ ] #9 Given the compose file, the installer of P4-E01-T02, the nginx site file, the release script and the release workflow in git When they are read Then wherever they name the app directory, the settings file or the web root, they name /opt/wedding-portal, /opt/wedding-portal/.env and /opt/wedding-portal/web, and none names another path for any of the three.
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
