---
id: TASK-30.5
title: >-
  P4-E01-T05 No portal token, invite token, access code or admin address in any
  server log
status: To Do
assignee: []
created_date: '2026-10-07 07:18'
labels:
  - technical
  - P4
  - stop-crypto-logging
  - stop-production-config
milestone: m-4
dependencies:
  - TASK-30.4
references:
  - 'rekord-api/deploy/nginx/rekord.conf:11-15'
  - 'rekord-api/deploy/nginx/rekord.conf:62-63'
  - 'rekord-api/src/main/java/app/rekord/account/Bootstrap.java:125'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:246'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/analysis/16-build-test-deploy-ops.md
parent_task_id: TASK-30
priority: high
type: task
ordinal: 40105
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the proxy and container logs of the release free of tokens, access codes and the first admin's address so that a leaked log opens no wedding.

H10 and RISK-13: guest tokens in /api/guest/<token> and invite tokens in /api/invites/<token> are written to the host nginx and container access logs (16 R-08); rekord-api's nginx conf redacts only /g/ (rekord-api/deploy/nginx/rekord.conf:11-15, :62-63), which P4-E01-T06 keeps for the magic-link pages and extends to the invite page /invite/<token>; the first-start bootstrap logs the planner's address (rekord-api/src/main/java/app/rekord/account/Bootstrap.java:125). The application side is P0-E06-T05; this ticket closes the proxy side and checks the whole release configuration in CI; P4-E04-T02 repeats the log check on the new server. Logging decisions and release settings are STOP items.

- STOP (human approval in the pull request): crypto-logging, production-config
- Covers: H10, RISK-13

Plan item `P4-E01-T05` (technical,P4,stop-crypto-logging,stop-production-config) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the nginx site file of wedding-portal When a request to /api/guest/<token> or /api/invites/<token> is written to the access log Then the logged path is /api/guest/[redacted] or /api/invites/[redacted] (the placeholder of rekord-api/deploy/nginx/rekord.conf:13).
- [ ] #2 Given a smoke run against the compose stack started in CI behind the nginx container of P4-E01-T02 that calls the portal with a test portal token, opens an invite with a test invite token and sends a test access code When the nginx access log, the nginx error log and the container logs of the run are searched Then none of the three values appears.
- [ ] #3 Given the first start of the compose stack in CI on an empty database with the first admin set as admin@example.com in the test settings When the container log of that start is searched Then neither the address nor the password appears (fixed defect; rekord-api logs the address at Bootstrap.java:125).
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
