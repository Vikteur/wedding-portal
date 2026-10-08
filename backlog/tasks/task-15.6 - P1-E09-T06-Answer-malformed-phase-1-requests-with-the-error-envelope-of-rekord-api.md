---
id: TASK-15.6
title: >-
  P1-E09-T06 Answer malformed phase-1 requests with the error envelope of
  rekord-api
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
milestone: m-1
dependencies:
  - TASK-15.1
  - TASK-9.3
  - TASK-10.2
  - TASK-14.2
  - TASK-15.3
  - TASK-15.5
references:
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:33-48'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:80-121'
  - 'docs/rewrite/analysis/10-identity-access.md:142-145'
  - 'docs/rewrite/analysis/11-planner-weddings.md:203'
  - 'rekord-contract/components/common.yaml:36-46'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10906
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want malformed requests on phase-1 routes to answer with the envelope rekord-api sends so that the planner app reads every refusal the same way.

Every error is {"detail":{"code","message"}} with a code from the fixed enum (BR-ID-40). The mappers of P0-E05-T02 already answer per exception family; this ticket asserts them on the phase-1 routes. A failed path or query conversion and an unknown route answer 404 NO_WEDDING "There is nothing here." whatever the resource (BR-ID-42, ErrorMappers.java:80-86, 10-identity-access.md:142-145). A body that is not JSON answers 422 VALIDATION_FAILED with one errors item {field null, code INVALID_FORMAT}, a JSON value of the wrong type answers 422 with the item {field <name>, code INVALID_FORMAT}, and an unknown enum value answers 422 with the item {field <name>, code INVALID_VALUE} (deviation UD-19.d3, built in P0-E05-T04; rekord-api answers 400 UNKNOWN). Any other unmapped framework refusal below 500, such as a wrong method or a wrong media type, keeps its status with UNKNOWN "That request could not be handled." (BR-ID-41, ErrorMappers.java:109-121). A bean-validation violation answers 422 VALIDATION_FAILED with "<last path segment> <message>" joined by "; " (BR-ID-43, BR-PL-55), plus the errors list of UD-12 with one item per violated field (P0-E05-T04). The contract describes UNKNOWN as never sent by the server (common.yaml:36-46); wedding-portal follows rekord-api, which sends it, and the contract text is left as it is (PIN-10-0399). The answers marked PIN(test) in the analysis are first recorded from rekord-api.

- Covers: BR-ID-40, BR-ID-41, BR-ID-42, BR-ID-43, PIN-10-0312, PIN-10-0313, PIN-10-0399, PIN-10-0145, PIN-11-0203, BR-PL-55

Plan item `P1-E09-T06` (technical,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner When the planner calls GET /api/weddings/not-a-uuid, GET /api/tasks?weddingId=x, GET /api/vendors/not-a-uuid and GET /api/not-a-route Then each answer is 404 {"detail":{"code":"NO_WEDDING","message":"There is nothing here."}} (BR-ID-42, PIN-10-0312, PIN-10-0145, PIN-11-0203).
- [ ] #2 Given rekord-api started from its repository in a test run and a planner When a characterization test sends POST /api/weddings with the body {, DELETE /api/weddings and POST /api/weddings with Content-Type text/plain Then it records each status and body, wedding-portal answers the second and third with the same status and body, and wedding-portal answers the first with 422 VALIDATION_FAILED whose errors list holds the one item {field null, code INVALID_FORMAT} (BR-ID-41, PIN-10-0399; deviation UD-19.d3, rekord-api answers 400 UNKNOWN).
- [ ] #3 Given the recorded answers When the test reads them Then the second and third hold the code UNKNOWN and the message written at ErrorMappers.java:116 with the status 405 and 415 in turn, the first recording holds 400 UNKNOWN, and a differing recording fails the test with both answers shown.
- [ ] #4 Given a planner When the planner calls createVendor with name "" and createWedding with the body {} Then both answer 422 VALIDATION_FAILED whose message names each violated field as "<field> <message>" joined by "; ", the first with the errors item {field name, code TOO_SHORT} and the second with {field couple_display_name, code REQUIRED} and {field wedding_date, code REQUIRED}, and nothing is stored (BR-ID-43, BR-PL-55, PIN-10-0313, UD-12, UD-19.d1).
- [ ] #5 Given the error answers produced by the criteria of this ticket, of P1-E09-T01, of the access-matrix test of P1-E09-T03 and of the isolation test of P1-E09-T05 When the contract test validates them Then each body is valid against the Error schema of the contract and its code is in the ErrorCode enum (BR-ID-40).
- [ ] #6 Given a planner When the planner calls createTask with the body {"title": 7} and with the body {"title": "Call the florist", "status": "PARKED"} Then the first answers 422 VALIDATION_FAILED with the errors item {field title, code INVALID_FORMAT} and the second with {field status, code INVALID_VALUE}, and no task is stored (deviation UD-19.d3; rekord-api answers 400 UNKNOWN).
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
