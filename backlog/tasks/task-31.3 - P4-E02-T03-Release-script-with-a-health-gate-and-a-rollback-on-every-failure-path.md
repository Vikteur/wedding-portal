---
id: TASK-31.3
title: >-
  P4-E02-T03 Release script with a health gate and a rollback on every failure
  path
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - user-story
  - P4
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-31.2
references:
  - 'rekord-api/deploy/deploy.sh:9-83'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:241-242'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:309'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-31
priority: high
type: feature
ordinal: 40203
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want a failed release to bring the previous version of wedding-portal back so that the app I work in stays available.

BR-OPS-11 and PIN-16-0146: a release is accepted only if GET http://127.0.0.1:$APP_PORT/api/health answers within 45 attempts 2 s apart; otherwise APP_IMAGE is restored and the script exits non-zero (rekord-api/deploy/deploy.sh:13-14, :50-78). H5, RISK-53 and PIN-16-0309: under set -euo pipefail a failed pull or a failed up exits before that rollback, leaving the new tag in the settings file (deploy.sh:9, :39-48; 16 R-04). RISK-52: the last Python run never reached the server (16 R-03). The previous image's tag is read before the switch (deploy.sh:35). A STOP item (production configuration). UD-19.n2, UD-19.n3 and UD-19.b: before docker compose pull the script runs a pre-flight step that reads /opt/wedding-portal/.env and stops while PUBLIC_HOST, BACKUP_TARGET, BACKUP_SCHEDULE or BACKUP_RETENTION_DAYS is unset or empty, or while the certificate file cloudflare-origin.pem or the key file cloudflare-origin.key is missing. The stops are proven here in CI, so the single release on the new server (UD-13.d) is checked from the pre-flight lines of its run log (P4-E04-T02) instead of by failing production runs.

- STOP (human approval in the pull request): production-config
- Covers: BR-OPS-11, PIN-16-0146, PIN-16-0309, H5, RISK-52, RISK-53, UD-19.n2, UD-19.n3

Plan item `P4-E02-T03` (user-story,P4,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a new image whose /api/health never answers When the release script runs Then it tries 45 times 2 s apart, sets APP_IMAGE back to the previous tag, starts the previous version, which answers GET /api/health with 200, and exits non-zero.
- [ ] #2 Given a new image that answers GET /api/health with 200 within the retry budget When the release script runs Then it exits 0 and APP_IMAGE holds the new tag.
- [ ] #3 Given docker compose up forced to fail after the app container was recreated When the release script runs Then APP_IMAGE is set back to the previous tag, the previous version answers GET /api/health with 200, and the script exits non-zero.
- [ ] #4 Given docker compose pull forced to fail When the release script runs Then APP_IMAGE is set back to the previous tag, the running version keeps answering, and the script exits non-zero.
- [ ] #5 Given the release script running under set -euo pipefail When any command between the tag switch and the health gate fails Then the rollback step runs before the script exits.
- [ ] #6 Given a server that does not answer on SSH When the release job runs Then the job fails red at the connection step, and afterwards APP_IMAGE in the server's settings file and the image of the running app container are unchanged from the values recorded when the run started.
- [ ] #7 Given a successful release When the clean-up step runs Then it removes only images older than 168 h and the image of the previous tag stays available (docker image prune -af --filter until=168h; rekord-api/deploy/deploy.sh:81).
- [ ] #8 Given /opt/wedding-portal/.env on the CI runner copied from ci/test.env with exactly one of PUBLIC_HOST, BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS set empty, in four runs one key each, When the release script runs Then each run exits non-zero naming that key before docker compose pull, and APP_IMAGE and the image of the running app container are unchanged (UD-19.n2, UD-19.n3).
- [ ] #9 Given /opt/wedding-portal/.env on the CI runner copied from ci/test.env and the key file cloudflare-origin.key missing, and in a second run the certificate file cloudflare-origin.pem missing When the release script runs Then each run exits non-zero naming the missing file before docker compose pull, and APP_IMAGE is unchanged (UD-19.b).
- [ ] #10 Given /opt/wedding-portal/.env with all four keys set and both certificate files present When the release script runs Then before docker compose pull its pre-flight step writes into the run log one line per key naming the key and the word set, without its value, and one line per certificate file naming the file and the word present.
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
