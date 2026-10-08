---
id: TASK-1.3
title: >-
  P0-E01-T03 Fast-jar container image on the eclipse-temurin 25 JRE running as
  uid 10001
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 08:54'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-1.2
references:
  - 'rekord-api/Dockerfile:18-71'
  - 'docs/rewrite/architecture-conventions.md:1003-1029'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-1
priority: high
type: task
ordinal: 103
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the Gradle-built fast-jar packed into a non-root Java 25 runtime image with a health check so that the image phase 4 ships is proven from phase 0.

rekord-api packs its Quarkus fast-jar on eclipse-temurin:25-jre as a non-root user with a curl health check (rekord-api/Dockerfile:18-71); PIN-AC-1008 asks whether the fast-jar built by Gradle starts on that image. BR-OPS-26 asks that every image runs as non-root: for wedding-portal that is this one image, while the nginx images of the frontends (H2) are deferred frontend matters. Phase 0 builds the image in CI and never ships it to a server (UD-13.e).

- Covers: PIN-AC-1008, BR-OPS-26

Plan item `P0-E01-T03` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the Dockerfile of wedding-portal When CI builds the image Then the build stage runs the Gradle build on JDK 25 with tests skipped, and the runtime stage is eclipse-temurin:25-jre holding /app/lib, /app/app, /app/quarkus and /app/quarkus-run.jar from the fast-jar.
- [x] #2 Given the built image When its configuration is inspected Then USER is 10001:10001, JAVA_OPTS is -XX:MaxRAMPercentage=70 -Duser.timezone=UTC, port 8080 is exposed and the entrypoint runs java $JAVA_OPTS -jar /app/quarkus-run.jar (rekord-api/Dockerfile:50-71).
- [x] #3 Given the built image When its health check is inspected Then it runs curl -fsS --max-time 3 http://127.0.0.1:8080/api/health every 30 s with a 5 s timeout, a 40 s start period and 3 retries (rekord-api/Dockerfile:68-69).
- [x] #4 Given a container started from the image in CI When its health check has run Then the container reports healthy, its Java process runs as uid 10001, and GET /api/health answers 200 {"ok":true}.
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
Finalization (2026-10-07): PR #3 merged as 531b1ff; CI run 37596108025 on head 9a6bcac green, jobs build and image.
- AC #1-#3 evidence: DockerfileTest (build stage on 25-jdk with -x test -x integrationTest, runtime 25-jre with the fast-jar under /app; USER 10001:10001, JAVA_OPTS, EXPOSE 8080, entrypoint; health check 30s/5s/40s/3) plus .github/scripts/image-check.sh in CI job image, which inspects the built image config.
- AC #4 evidence: image-check.sh in the same CI run started the container, waited for healthy, read uid/gid 10001 of PID 1 (java) and got 200 {"ok":true} from /api/health, compared with fixtures/health-200.json; log ends "OK: wedding-portal:ci".
- image-check.sh was mutation-tested locally (USER 0, a 10 s interval, wrong JAVA_OPTS): each variant failed. DockerfileTest.image_check_script_asserts_every_acceptance_criterion was never seen red.
- DoD #3 N/A: no ArchUnit suite exists yet (it arrives with TASK-3.1).
- DoD #4 N/A: no contract change; the ticket adds no operation.
- DoD #5 N/A: touches no STOP item; the image is never pushed, no registry login and no secret (UD-13.e).
- DoD #6 N/A: no refusal path; the image adds no operation.
- DoD #7: the only operation, GET /api/health, answers the rekord-api fixture body from the running container.
- DoD #9 N/A: no schema change.
- Follow-up: the docs/memory.md entry in wedding-portal still says the CI run "is still pending"; it is green (run 37596108025).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the multi-stage Dockerfile (Gradle fast-jar build on eclipse-temurin:25-jdk, runtime on eclipse-temurin:25-jre as uid/gid 10001 with JAVA_OPTS, port 8080 and a curl health check), a deny-by-default .dockerignore, and a CI job image that builds the image and runs .github/scripts/image-check.sh against its config and a running container. Verified by DockerfileTest and by the green image job of CI run 37596108025 on SHA 9a6bcac (healthy, PID 1 uid 10001, GET /api/health 200 {"ok":true}); PR #3 merged as 531b1ff.
<!-- SECTION:FINAL_SUMMARY:END -->
