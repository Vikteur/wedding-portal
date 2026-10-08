---
id: TASK-6.4
title: P0-E06-T04 CORS off in the prod profile and limited to DEV_CORS_ORIGINS in dev
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-6.1
  - TASK-4.5
references:
  - 'rekord-api/src/main/resources/application.properties:57-81'
  - 'rekord-contract/dist/openapi.yaml:45-46'
  - 'rekord-api/src/main/java/app/rekord/ApiApplication.java:9-19'
  - 'rekord-contract/dist/openapi.yaml:76'
  - 'rekord-contract/dist/openapi.yaml:1972'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: task
ordinal: 604
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want cross-origin calls refused outside dev and allowed only for the vite origins in dev so that the apps keep calling /api on their own origin.

BR-OPS-24: rekord-api turns CORS off in %prod and allows only DEV_CORS_ORIGINS with credentials in %dev, defaulting to the vite ports 5173 to 5176 (rekord-api/src/main/resources/application.properties:57-65, :81). BR-OPS-18: the frontends call /api on their own origin, through the vite proxy in dev, and stay unchanged; the contract's server URL is /api (rekord-contract/dist/openapi.yaml:45-46), while its paths carry no /api prefix (rekord-contract/dist/openapi.yaml:76, :1972). rekord-api therefore roots its resources with @ApplicationPath("/api") instead of quarkus.http.root-path, which would also move /q/health and /q/metrics, the endpoints the container health check and the release gate read (rekord-api/src/main/java/app/rekord/ApiApplication.java:9-19).

- Covers: BR-OPS-24, BR-OPS-18

Plan item `P0-E06-T04` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the application in the prod profile When a browser sends a request with an Origin header of another origin and credentials Then the answer carries no Access-Control-Allow-Origin and no Access-Control-Allow-Credentials header.
- [ ] #2 Given the dev profile with DEV_CORS_ORIGINS unset When a request comes from one of http://localhost:5173 to http://localhost:5176 or http://127.0.0.1:5173 to http://127.0.0.1:5176 Then the answer allows that origin with credentials.
- [ ] #3 Given the dev profile with DEV_CORS_ORIGINS set to http://localhost:5180 When a request comes from http://localhost:5173 Then that origin is not allowed, while a request from http://localhost:5180 is allowed with credentials.
- [ ] #4 Given the dev profile When a cross-origin POST comes from an origin outside the dev list Then the answer is 403 with no body (rekord-api/src/main/resources/application.properties:57-62).
- [ ] #5 Given the application When its routes are listed Then every generated operation is served under /api through one jakarta.ws.rs.core.Application subclass annotated @ApplicationPath("/api") (rekord-api/src/main/java/app/rekord/ApiApplication.java:19), quarkus.http.root-path and quarkus.rest.path are unset, GET /api/health answers 200 and GET /q/health/ready answers at /q/health/ready.
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
