---
id: TASK-5.2
title: >-
  P0-E05-T02 Error envelope mapper answering byte for byte as rekord-api per
  exception family
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - technical
  - P0
  - stop-crypto-logging
milestone: m-0
dependencies:
  - TASK-5.1
references:
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:17-122'
  - 'docs/rewrite/architecture-conventions.md:778-798'
  - 'docs/rewrite/analysis/08-api-behaviour.md:104-108'
  - 'docs/rewrite/architecture-conventions.md:505-512'
  - 'rekord-api/src/main/java/app/rekord/ApiApplication.java:15-19'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: task
ordinal: 502
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want every exception family and every framework exception answered with the envelope rekord-api sends so that the unchanged apps read each refusal without a change.

The apps parse {"detail":{"code","message"}}, deliberately not RFC 7807 (rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:17-25; PIN-18-0549). PIN-AC-0767 asks one response per family recorded from rekord-api and compared byte for byte (a written departure from field-level assertions, CT-23); PIN-18-0658 adds the generic 500. The framework-raised answers come from architecture-conventions §11.2. The catch-all writes the cause to the server log only (ErrorMappers.java:118-119), a decision about what is logged, so the ticket is a STOP item; UD-19.f keeps every token, access code, password, e-mail address, person's name and phone number out of that line.

- STOP (human approval in the pull request): crypto-logging
- Covers: PIN-18-0549, PIN-18-0658, PIN-AC-0767, UD-19.f

Plan item `P0-E05-T02` (technical,P0,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an AuthenticationFailedException or an UnauthorizedException When the mapper answers Then the status is 401 and the body is {"detail":{"code":"NOT_SIGNED_IN","message":"Sign in to continue."}}, equal byte for byte to the response recorded from rekord-api.
- [ ] #2 Given a jakarta.ws.rs.ForbiddenException When the mapper answers Then the status is 403 with the code FORBIDDEN and the message "This is not yours to open.", equal byte for byte to the recorded response.
- [ ] #3 Given a test-only resource method that throws, in turn, NotFoundException, RejectedException, NotPermittedException and UpstreamUnavailableException, each with a code and message When the mapper answers Then status and body equal byte for byte the response recorded from rekord-api for an ApiException with the same status, code and message (rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:43-47).
- [ ] #4 Given a request to a route no resource declares, GET /api/q/metrics among them (rekord-api/src/main/java/app/rekord/ApiApplication.java:15-19), When the mapper answers Then the status is 404 and the body is {"detail":{"code":"NO_WEDDING","message":"There is nothing here."}}, equal byte for byte to the response recorded from rekord-api.
- [ ] #5 Given a request whose path UUID is not a valid UUID When the mapper answers Then the status is 404 with the same body, equal byte for byte to the response recorded from rekord-api for that request.
- [ ] #6 Given a request whose generated DTO breaks two bean-validation constraints When the mapper answers Then the status is 422 with the code VALIDATION_FAILED and a message of each violation as its last path segment, a space and its message, without duplicates, joined by "; " (ErrorMappers.java:89-103).
- [ ] #7 Given a WebApplicationException with a status below 500 that no other mapper names and that is not raised by an unreadable or mistyped request body (answered 422 VALIDATION_FAILED in P0-E05-T06, UD-19.d3) When the catch-all answers Then the status is kept, the code is UNKNOWN and the message is the one at ErrorMappers.java:116.
- [ ] #8 Given any other exception When the catch-all answers Then the status is 500 with the code UNKNOWN and the message "Something went wrong at our end.", the body holds no stack trace, class name, SQL or table name, and the cause is written once to the server-side log at ERROR with no value UD-19.f keeps out of logs in the line or its stack trace (checked in P0-E06-T05).
- [ ] #9 Given a request body of 10240 KiB plus one byte When it reaches the HTTP layer Then the answer is 413 with no error envelope, and application.properties sets quarkus.http.limits.max-body-size=10240K explicitly (the Quarkus default rekord-api relies on; docs/rewrite/architecture-conventions.md:505-512).
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
