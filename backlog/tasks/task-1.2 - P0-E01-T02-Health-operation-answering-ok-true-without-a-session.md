---
id: TASK-1.2
title: 'P0-E01-T02 Health operation answering {"ok":true} without a session'
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 08:28'
labels:
  - user-story
  - P0
milestone: m-0
dependencies:
  - TASK-1.1
references:
  - 'rekord-api/src/main/java/app/rekord/HealthResource.java:1-21'
  - 'rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-64'
  - 'rekord-contract/dist/openapi.yaml:1972'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-1
priority: high
type: feature
ordinal: 102
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a visitor, I want GET /api/health to answer without a session so that anyone checking whether wedding-portal runs gets a plain yes.

health is the only operation of phase 0 (OP:health; UD-15.d). rekord-api answers it from HealthResource with {"ok":true}, unauthenticated, polled by the container health check and the release health gate (rekord-api/src/main/java/app/rekord/HealthResource.java:7-20), and RouteGuardTest keeps it on the public list (rekord-api/src/test/java/app/rekord/security/RouteGuardTest.java:49-64). rekord-api tests it, so the ticket is not test-first. Codegen arrives in P0-E02; P0-E02-T03 then makes this resource implement the generated HealthApi interface. That health checks no dependency (BR-OPS-21) is proven in P0-E04-T05, once a database exists.

- Builds: `health`

Plan item `P0-E01-T02` (user-story,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a visitor without a session or cookie When the visitor calls the health operation health (GET /api/health) Then the answer is 200 with Content-Type application/json and exactly the body {"ok":true} (body: rekord-api/src/main/java/app/rekord/HealthResource.java:15-19; Content-Type: the application/json response of operation health in rekord-contract/dist/openapi.yaml).
- [x] #2 Given rekord-api run locally with ./mvnw quarkus:dev and the status, the Content-Type header and the body of its answer to GET /api/health stored as application/src/test/resources/fixtures/health-200.json in wedding-portal When a @QuarkusTest of wedding-portal sends the same request Then the status, the Content-Type header and the body equal the fixture byte for byte.
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [x] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [x] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [x] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [x] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [x] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Built by the Archon build-feature workflow (plan and review on Opus 5.5, code and tests on Sonnet 5.5); the plan lived in the run's plan.md artifact. Run 078ca850 failed in the implement loop because the plan held a push/PR/CI step; run 015b9131 adopted its branch and finished after the workflow fix (umbrella 4f46ddb, wedding-portal 7170288). Verification: wedding-portal PR #2, CI run 37592928463 green on head SHA 3d0c753 (./gradlew build incl. :application:integrationTest, which runs **/*IT, plus the required-task and fast-jar steps; the failure-only "Upload test reports" step was skipped as designed); merged as 00a2a8d on 2026-10-07. AC evidence: HealthResourceIT.answers_200_with_json_ok_true_to_a_visitor_without_a_session (AC1: 200, application/json, body bytes {"ok":true}, no cookie) and HealthResourceIT.answers_exactly_what_rekord_api_answers (AC2: status, raw Content-Type and body bytes equal fixtures/health-200.json, captured from rekord-api ec65ae35 with ./mvnw quarkus:dev: 200, application/json;charset=UTF-8, {"ok":true}). DoD #1: in the run both tests failed under each mutation (Health(false), @Produces text/plain, @Path /healthz; the last fails on expected 200 but was 404). DoD #7: health matches rekord-api byte for byte (AC2), no deviation implemented. DoD #8: the fixture holds only status, Content-Type and {"ok":true}. DoD #10: docs/memory.md records the /api root, the fixture provenance and the temporary hand-written resource. DoD not applicable and checked as such: #3 (no ArchUnit suite yet; it arrives in TASK-3.1, so no baseline entries), #4 (no contract change; HealthResource is hand-written until P0-E02-T03, no generated file exists or was edited), #5 (no STOP item touched; the STOP gate was skipped), #6 (health has no refusal path), #9 (no schema change).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
GET /api/health answers 200, application/json;charset=UTF-8, {"ok":true} without a session: HealthResource and a temporary Health record in application, rooted at /api by ApiApplication (@ApplicationPath, so /q/* stays at the root), with quarkus-rest-jackson and rest-assured added. HealthResourceIT checks AC1 and compares status, Content-Type and body byte for byte with a fixture captured from rekord-api (AC2). Verified by CI run 37592928463 green on PR #2 head 3d0c753, merged as 00a2a8d. P0-E02-T03 replaces the hand-written resource with the generated HealthApi.
<!-- SECTION:FINAL_SUMMARY:END -->
