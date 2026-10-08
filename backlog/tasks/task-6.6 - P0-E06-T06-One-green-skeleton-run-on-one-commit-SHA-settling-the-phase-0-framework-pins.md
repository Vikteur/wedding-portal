---
id: TASK-6.6
title: >-
  P0-E06-T06 One green skeleton run on one commit SHA settling the phase-0
  framework pins
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-5.6
  - TASK-6.1
  - TASK-6.2
  - TASK-6.3
  - TASK-6.4
  - TASK-6.5
references:
  - 'docs/rewrite/architecture-conventions.md:1109-1150'
  - 'docs/rewrite/STATUS.md:279-297'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-6
priority: high
type: task
ordinal: 606
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one CI run of the finished skeleton to prove every framework pin of phase 0 so that phase 1 starts on a proven base.

CONV-7 and PIN-AC-0104: one CI run of the skeleton settles the framework PIN(test) items of architecture-conventions (Java 25 and the Quarkus line under Gradle, Jandex discovery, ArchUnit on Java 25 bytecode, the generated output path, the rollback test, the datasource-less profile, WireMock 3, the 403 body, the envelope per family, the fast-jar on the Java 25 image), fetched by SHA (FW-P-04). PIN-AC-1266 lists where UD-2 applies. UD-13.e: phase 0 has CI only and ships nothing to a server.

- Covers: CONV-7, PIN-AC-0104, PIN-AC-1266, UD-13.e

Plan item `P0-E06-T06` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the skeleton with every other phase-0 ticket merged When CI runs on one commit SHA of main Then that one run builds with Gradle on the Java 25 toolchain with the Quarkus line pinned in the version catalog, starts @QuarkusTest, and runs openApiGenerate, Hibernate validate, the V1 schema baseline, ArchUnit and Testcontainers green, and the result is fetched by that SHA.
- [ ] #2 Given that run When its test reports are read Then the Jandex discovery test, ArchUnit on Java 25 bytecode, the generated output path, both rollback tests, the datasource-less profile, WireMock 3, the 403 body, the envelope per family and the fast-jar image start each passed.
- [ ] #3 Given the version catalog When that run is green Then the Quarkus line it proved is the one pinned, and docs/memory.md records the run's SHA as the proof.
- [ ] #4 Given the CI workflows of wedding-portal at the end of phase 0 When they are listed Then none ships an image to a server or opens a connection to one (UD-13.e).
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
