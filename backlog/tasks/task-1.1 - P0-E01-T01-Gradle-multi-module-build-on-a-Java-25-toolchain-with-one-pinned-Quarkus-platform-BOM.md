---
id: TASK-1.1
title: >-
  P0-E01-T01 Gradle multi-module build on a Java 25 toolchain with one pinned
  Quarkus platform BOM
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 07:44'
labels:
  - technical
  - P0
milestone: m-0
dependencies: []
references:
  - 'docs/rewrite/STATUS.md:226-239'
  - 'rekord-api/pom.xml:11-16'
  - 'docs/rewrite/architecture-conventions.md:68-155'
  - 'docs/rewrite/architecture-conventions.md:1003-1029'
  - 'docs/rewrite/analysis/18-framework-code-rulebook.md:616'
  - 'docs/rewrite/architecture-conventions.md:1250-1257'
  - 'docs/rewrite/STATUS.md:346-347'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-1
priority: high
type: task
ordinal: 101
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want one Gradle build of six modules on a Java 25 toolchain with one pinned Quarkus platform BOM so that every later ticket compiles against the same proven framework line.

UD-1, UD-2, UD-4 and UD-5 fix Quarkus, Java 25, Gradle and the new repository wedding-portal; UD-3 and UD-13.a make rekord-api the behaviour oracle only, never a dependency or something that is shipped; UD-8 rules out Python. The Quarkus line is the open PIN(test) P-1 of slice 18 (PIN-AC-1255 and the four answered rows that point at it): rekord-api runs Quarkus 3.39.1 on Java 25 under Maven (rekord-api/pom.xml:11, :16), and only a green Gradle run, read by commit SHA, proves the line for Gradle. Module layout and Jandex discovery follow architecture-conventions §2.2 and §14.1 (PIN-AC-0153). UD-17: tickets are executor-neutral and the user chooses the executor before the first ticket starts, so this first ticket records that choice.

- Covers: UD-1, UD-2, UD-3, UD-4, UD-5, UD-8, UD-13.a, PIN-AC-0079, PIN-AC-1255, PIN-AC-1305, PIN-AC-1308, PIN-AC-1315, PIN-AC-1317, PIN-18-0535, PIN-AC-0153, UD-17

Plan item `P0-E01-T01` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the wedding-portal repository When settings.gradle.kts is read Then it includes exactly the modules rekord-domain, rekord-usecase, rekord-adapter, rekord-gateway, application and logging, written in the Kotlin DSL, and only application applies the Quarkus Gradle plugin.
- [x] #2 Given gradle/libs.versions.toml When the build resolves its dependencies Then one Quarkus platform BOM is imported at version 3.39.1, the line rekord-api runs (rekord-api/pom.xml:16), no module declares a Quarkus artifact version of its own, and if the Gradle build on Java 25 is red at 3.39.1, the first newer Quarkus line that turns it green is pinned instead and recorded in docs/memory.md (PIN-AC-1255).
- [x] #3 Given the Java toolchain and the compiler release set to 25 for every module When CI runs ./gradlew build Then every compiled class file has class-file major version 69 and the run on that commit SHA is green.
- [x] #4 Given one bean class each in rekord-usecase, rekord-adapter and rekord-gateway, discovered through a Jandex index of its module When a @QuarkusTest in application injects all three Then the test passes.
- [x] #5 Given the Gradle build When CI lists its tasks Then test, integrationTest, build and :application:quarkusBuild exist, and quarkusBuild produces a fast-jar under application/build/quarkus-app.
- [x] #6 Given the wedding-portal repository When its files and CI workflows are listed Then no .py file and no Python step exist, no dependency names a rekord-api artifact, and no workflow builds or ships a rekord-api image (UD-8, UD-13.a).
- [x] #7 Given the wedding-portal repository with the skeleton merged When docs/memory.md is read Then it names the executor the user chose for the tickets (UD-17).
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
Built by the Archon build-feature workflow (plan and review on Opus 5.5, code and tests on Sonnet 5.5); the plan lived in the run's plan.md artifact. Verification: wedding-portal PR #1, CI run green on head SHA 44185bf (./gradlew build incl. integrationTest, required-task and fast-jar steps); merged as a2afd09 on 2026-10-07. AC evidence: BuildLayoutTest (AC1, AC2, AC3 toolchain, AC5 CI steps, AC6, AC7), ClassFileVersionTest (AC3 major version 69), ModuleBeanDiscoveryIT (AC4), CI steps 'Required tasks exist' and 'Fast-jar built' (AC5). Quarkus 3.39.1 was green on Gradle and Java 25, so no newer line was needed (PIN-AC-1255). DoD not applicable to this skeleton and checked as such: #3 (no ArchUnit suite yet; it arrives in TASK-3.1, so no baseline entries), #4 (no contract change), #5 (no STOP item touched), #6 and #7 (no operation built), #9 (no schema change).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Skeleton of wedding-portal: Gradle 9.8.0 Kotlin-DSL build of six modules on a Java 25 toolchain (release 25), one pinned Quarkus 3.39.1 platform BOM, Jandex-indexed library modules, test and integrationTest tasks, fast-jar build and a Temurin 25 CI workflow; docs/memory.md records the executor (UD-17). Verified by BuildLayoutTest, ClassFileVersionTest and ModuleBeanDiscoveryIT and by a green CI run on SHA 44185bf; PR #1 merged as a2afd09.
<!-- SECTION:FINAL_SUMMARY:END -->
