---
id: TASK-4.5
title: >-
  P0-E04-T05 Readiness with a datasource check, Prometheus metrics and a
  dependency-free health
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-4.2
references:
  - 'rekord-api/pom.xml:86-89'
  - 'rekord-api/pom.xml:122-125'
  - 'rekord-api/src/main/java/app/rekord/HealthResource.java:7-20'
  - 'docs/rewrite/analysis/16-build-test-deploy-ops.md:197-198'
  - 'rekord-api/src/main/java/app/rekord/ApiApplication.java:15-19'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-4
priority: high
type: task
ordinal: 405
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want readiness to report the database and the health operation to ignore it so that the container gate and the readiness probe each answer their own question.

rekord-api exposes /q/health, /q/health/live and /q/health/ready (SmallRye Health; rekord-api/pom.xml:86-89) and /q/metrics (Prometheus; rekord-api/pom.xml:122-125) on loopback only; nginx proxies only /api/. Whether readiness includes the datasource rests on the extension default, so PIN-16-0197 and PIN-16-0198 pin it. BR-OPS-21: /api/health is unauthenticated, returns only {"ok":true} and checks no dependency. The envelope body of that 404 for GET /api/q/metrics, the unknown-route answer, is asserted in P0-E05-T02, which builds the mapper.

- Covers: PIN-16-0197, PIN-16-0198, BR-OPS-21

Plan item `P0-E04-T05` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the application running with its PostgreSQL container up When GET /q/health/ready is called Then the answer is 200 with status UP and the check named "Database connections health check" (quarkus-agroal) with status UP.
- [ ] #2 Given the application running and its PostgreSQL container stopped When GET /q/health/ready is called Then the answer is 503 with status DOWN and the check named "Database connections health check" (quarkus-agroal) with status DOWN.
- [ ] #3 Given the PostgreSQL container stopped When a visitor calls health (GET /api/health) Then the answer is 200 {"ok":true}, and the health resource injects no datasource, repository or EntityManager.
- [ ] #4 Given the running application When GET /q/metrics is called Then the answer is 200 in the Prometheus text format with at least one line starting with # TYPE.
- [ ] #5 Given the running application When GET /api/q/metrics is called Then the answer is 404 and its body holds no metrics text and no line starting with # TYPE, because /q stays outside the /api root (rekord-api/src/main/java/app/rekord/ApiApplication.java:15-19).
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
