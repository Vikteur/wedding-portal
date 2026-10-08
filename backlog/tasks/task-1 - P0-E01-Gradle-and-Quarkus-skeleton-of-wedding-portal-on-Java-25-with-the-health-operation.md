---
id: TASK-1
title: >-
  P0-E01 Gradle and Quarkus skeleton of wedding-portal on Java 25 with the
  health operation
status: Done
assignee: []
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 08:54'
labels:
  - epic
  - P0
milestone: m-0
dependencies: []
references:
  - 'docs/rewrite/STATUS.md:226-239'
  - 'docs/rewrite/architecture-conventions.md:68-155'
  - 'docs/rewrite/architecture-conventions.md:1003-1029'
  - 'rekord-api/src/main/java/app/rekord/HealthResource.java:1-21'
  - docs/rewrite/backlog-plan/digest-P0.md
  - 'docs/rewrite/STATUS.md:346-347'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want a Gradle multi-module Quarkus build on a Java 25 toolchain that starts and answers the health operation so that every later ticket builds on a proven skeleton.

Goal: the new repository wedding-portal (UD-5) exists with the module layout of architecture-conventions §2.2, built by Gradle (UD-4) on Java 25 (UD-2) with Quarkus (UD-1), written from scratch with rekord-api as the behaviour oracle only (UD-3, UD-13.a), with no Python in anything it needs to build, test or run (UD-8). Scope: the settings file and the version catalog, the modules rekord-domain, rekord-usecase, rekord-adapter, rekord-gateway, application and logging, Jandex bean discovery, the Quarkus fast-jar on a Java 25 image, the health operation (the only phase-0 operation, UD-15.d); the executor the user chose, recorded in docs/memory.md by the first ticket (UD-17); the SmallRye health and metrics endpoints rekord-api exposes come with the datasource in P0-E04. Out of scope: contract code generation (P0-E02), ArchUnit (P0-E03), the database (P0-E04) and any deploy: phase 0 has CI only (UD-13.e), so the deploy-script PIN items of the P0 digest move to P4.

Plan item `P0-E01` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the wedding-portal repository with the skeleton merged When CI builds it with Gradle on a Java 25 toolchain Then the modules rekord-domain, rekord-usecase, rekord-adapter, rekord-gateway, application and logging compile and a @QuarkusTest in application injects one bean from each of rekord-usecase, rekord-adapter and rekord-gateway.
- [x] #2 Given the application started from the Gradle-built fast-jar on the Java 25 runtime image When a visitor without a session calls the health operation GET /api/health Then the answer is 200 with the body {"ok":true} that rekord-api returns.
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
Epic closed (2026-10-07): sub-tickets TASK-1.1 (PR #1, a2afd09), TASK-1.2 (PR #2, 00a2a8d) and TASK-1.3 (PR #3, 531b1ff) are Done, each with CI green by head SHA.
- AC #1 evidence: TASK-1.1, BuildLayoutTest, ClassFileVersionTest and ModuleBeanDiscoveryIT (a @QuarkusTest injecting one bean from each of rekord-usecase, rekord-adapter and rekord-gateway), CI green on 44185bf.
- AC #2 evidence: TASK-1.3 image-check.sh in CI run 37596108025 on 9a6bcac started the Gradle-built fast-jar on the eclipse-temurin:25-jre image and got 200 {"ok":true} from GET /api/health without a session; TASK-1.2 HealthResourceIT covers the same answer in-process.
- DoD #3 N/A: no ArchUnit suite yet (TASK-3.1). DoD #4 N/A: no contract change. DoD #5 N/A: no STOP item touched. DoD #6 N/A: no refusal path. DoD #9 N/A: no schema change.
- DoD #2 and #11 hold per sub-ticket PR; the epic has no PR of its own.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Skeleton of wedding-portal done across three sub-tickets: Gradle multi-module Quarkus build on Java 25 (TASK-1.1), the health operation GET /api/health answering {"ok":true} (TASK-1.2) and the non-root fast-jar image on eclipse-temurin:25-jre (TASK-1.3). Verified by ModuleBeanDiscoveryIT, HealthResourceIT, DockerfileTest and the CI image check on a running container (run 37596108025, SHA 9a6bcac).
<!-- SECTION:FINAL_SUMMARY:END -->
