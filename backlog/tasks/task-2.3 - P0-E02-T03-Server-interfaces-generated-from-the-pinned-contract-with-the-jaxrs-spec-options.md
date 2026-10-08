---
id: TASK-2.3
title: >-
  P0-E02-T03 Server interfaces generated from the pinned contract with the
  jaxrs-spec options
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 14:56'
labels:
  - user-story
  - P0
milestone: m-0
dependencies:
  - TASK-2.2
references:
  - 'rekord-api/pom.xml:178-223'
  - 'docs/rewrite/architecture-conventions.md:232-262'
  - '.claude/hooks/guard-generated.globs:20-21'
  - 'docs/rewrite/analysis/18-framework-code-rulebook.md:550'
  - 'docs/rewrite/architecture-conventions.md:490-499'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:64-73'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-2
priority: high
type: feature
ordinal: 203
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want the server to answer exactly the shapes of the contract my app is built from so that the app never meets a field it does not know.

Contract-first (constitution P4): rekord-api implements interfaces that openapi-generator 7.25.0 generates with jaxrs-spec and fixed options, so drift from the spec is a compile error (BR-OPS-01; rekord-api/pom.xml:178-223). architecture-conventions §4 carries that table into Gradle. The output must sit under build/generated so the write guard covers it (18 C-23; PIN-18-0498, PIN-18-0550, PIN-AC-0261, PIN-AC-0263), and generated Java is never committed (BR-OPS-30; the TypeScript half of that rule belongs to the unchanged frontends, deferred with BR-OPS-07). The spec arrives as the contract.spec Gradle property from the checkout of P0-E02-T02 (BR-OPS-02; RD-02). Quarkus reads the JAX-RS metadata from the generated interface, so a 201 or 204 cannot come from an annotation on the implementing method; one helper sets it, the ArchUnit rule that no other class sets a status is P0-E03-T01, and every build ticket of phases 1 to 3 uses that helper, whose check over all operations is P4-E04-T01 (PIN-AC-0499; architecture-conventions §7.1).

- Covers: BR-OPS-01, BR-OPS-02, BR-OPS-30, PIN-18-0498, PIN-18-0550, PIN-AC-0261, PIN-AC-0263

Plan item `P0-E02-T03` (user-story,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the openApiGenerate task in rekord-adapter When it runs Then it uses openapi-generator 7.25.0 pinned in gradle/libs.versions.toml, the generator jaxrs-spec, the packages app.rekord.api and app.rekord.api.model, and the options interfaceOnly=true, useJakartaEe=true, returnResponse=false, useSwaggerAnnotations=false, openApiNullable=false, dateLibrary=java8 and useTags=true, with API and model tests off.
- [x] #2 Given a CI run When openApiGenerate has run Then the generated interfaces sit under rekord-adapter/build/generated/openapi, a path the guard globs */build/generated/* and build/generated/* match (.claude/hooks/guard-generated.globs:20-21), and that directory is a source root compileJava depends on.
- [x] #3 Given a clean build of wedding-portal When git ls-files is run afterwards Then no generated file is tracked and build/ is ignored.
- [x] #4 Given a resource implementing a generated interface When the contract renames an operation or a field the resource uses Then compileJava fails.
- [x] #5 Given a CI job of wedding-portal that generates code or starts the application, the image build included When it runs Then it passes contract.spec pointing at dist/openapi.yaml of the rekord-contract checkout at the pinned tag or the shared feature branch.
- [x] #6 Given the health resource of P0-E01-T02 When codegen is in place Then it implements the generated HealthApi interface and its 200 {"ok":true} test still passes.
- [x] #7 Given contract fields with format date and format date-time When the models are generated Then their Java types are LocalDate and OffsetDateTime.
- [x] #8 Given the helper in app.rekord.adapter.web.shared that sets the success status of a resource method from the success code the contract documents for its operation When a resource test calls a test-only resource method whose helper call names 201 and one whose helper call names 204 Then the first answers 201 with its body, and the second answers 204 with no body.
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
Finalization evidence (wedding-portal PR https://github.com/Vikteur/wedding-portal/pull/11, merged by the user as 68e7585da2edda999d917e2d9aaec29a3a16fe98; CI run 37634668615 on head cf5d5babe6236f36756aa703df6f3313914d2558: jobs build and image both success).
- AC #1: OpenApiGeneratorPinTest (7.25.0 in gradle/libs.versions.toml, plugin by catalog alias) and GeneratedContractTest (jaxrs-spec generator and version in every file, packages app.rekord.api / app.rekord.api.model, interfaceOnly, jakarta, no Response return, no io.swagger, no JsonNullable, one interface per spec tag and no DefaultApi, no API or model tests generated).
- AC #2: GeneratedContractTest checks HealthApi.java under rekord-adapter/build/generated/openapi, matching the globs */build/generated/* and build/generated/*, and HealthApi.class loaded from rekord-adapter/build. The CI log of run 37634668615 shows ":rekord-adapter:openApiGenerate ... Successfully generated code to .../rekord-adapter/build/generated/openapi".
- AC #3: GeneratedSourcesUntrackedTest (git ls-files tracks no build/ path and no app/rekord/api/ file; git check-ignore succeeds for the generated HealthApi).
- AC #4: ContractDriftBreaksCompileTest (a renamed operation and a renamed field each fail to compile HealthResource; the control compiles). GeneratedContractTest also checks that the generated APIs are exactly the spec's tags, so no stale type survives a spec change (cleanupOutput=true).
- AC #5: ContractSpecWiringTest. In the CI log, the build job runs "./gradlew test integrationTest build --stacktrace -Pcontract.spec=contract/dist/openapi.yaml", and the image job checks out rekord-contract at refs/tags/v0.1.0 (read-only and ref checks OK) and runs the Dockerfile gradlew build with -Pcontract.spec=contract/dist/openapi.yaml.
- AC #6: app.rekord.adapter.web.health.HealthResource implements the generated HealthApi (HealthResourceImplementsContractTest). HealthResourceIT is unchanged and green (200 {"ok":true}, byte for byte the rekord-api fixture), and image-check reports "OK: wedding-portal:ci".
- AC #7: GeneratedContractTest (Wedding.getWeddingDate() is LocalDate, PortalLink.getExpiresAt() is OffsetDateTime, no joda and no java.util.Date).
- AC #8: SuccessStatusIT on a test-only probe resource (201 with its body, 204 with no body, 200 when the helper is not called, a refusal after answer(201) keeps 409) and SuccessStatusTest (only 200/201/202/204 accepted).
- DoD #1: every AC above has a test that fails when the criterion is broken. The tests were committed before the code on each step, and the TDD gate passed.
- DoD #2: CI run 37634668615 is green on head cf5d5ba, read by SHA. No test was disabled. The only assumption in the suite (ContractPinTest, Windows without Git Bash) predates this ticket and does not apply on the ubuntu runner.
- DoD #3 N/A: no ArchUnit suite exists yet in wedding-portal (TASK-3.1 / P0-E03-T01 is To Do), so there is no frozen baseline to add to.
- DoD #4: no contract change was needed. The pin stays rekordContractTag=v0.1.0 (CI: "rekord-contract is at tag v0.1.0 (055b133)"). The generated files are untracked and were never edited (GeneratedSourcesUntrackedTest).
- DoD #5: the ticket touches no STOP item (no contract push, migration, auth, encryption or logging, history deletion or production config). The user's merge of PR #11 as 68e7585 records the review (UD-21.e).
- DoD #6 N/A: the ticket builds no refusal. The error envelope and Notification validation are P0-E05.
- DoD #7: health answers exactly as rekord-api does (HealthResourceIT against fixtures/health-200.json). No UD deviation was implemented.
- DoD #8: no secret appears in the code, tests or logs. CONTRACT_TOKEN stays a runner secret and never reaches docker build (ContractSpecWiringTest; CiWorkflowTest counts the secrets references).
- DoD #9 N/A: no schema change and no migration.
- DoD #11: PR #11 was reviewed and merged into main by the user as 68e7585 (UD-21.e).
- DoD #10 NOT MET, left unchecked: docs/code-maps/code-generation.md (added by TASK-3.3, PR #10, and merged into this branch at cf5d5ba) still says "not built yet: no `openApiGenerate` task exists in the tree today, and the generator version is not yet in `gradle/libs.versions.toml`". This ticket built both, so that leaf is now outdated, and PR #11 did not update it. It should also name the cleanupOutput setting, the -Pcontract.spec input and the SuccessStatus helper. The status stays In Progress until a follow-up PR updates that leaf (CodeMapLeavesTest still has to pass).
- Follow-ups (not created): update docs/code-maps/code-generation.md as above. The ArchUnit rule that no class other than SuccessStatus/SuccessStatusFilter sets a status is P0-E03-T01, and the check over all 201/204 operations is P4-E04-T01.

DoD #10 now met: wedding-portal PR https://github.com/Vikteur/wedding-portal/pull/12, merged by the user as 161b143, updated docs/code-maps/code-generation.md. The leaf now describes the built codegen: openApiGenerate in rekord-adapter, OpenAPI Generator 7.25.0 in the catalog, cleanupOutput, the src/gen/java source root and -Pcontract.spec. The change was test first: 95ec3a9 changed CodeMapLeavesTest alone, and its red run had 9 tests with 1 failure; 4db2b24 then changed the leaf. CI run 37638599179 on head 4db2b24 is green (build and image). The leaf on main no longer contains 'not built yet'.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Server interfaces are now generated from the pinned rekord-contract (v0.1.0). openApiGenerate in rekord-adapter uses OpenAPI Generator 7.25.0 (jaxrs-spec, interfaceOnly, packages app.rekord.api and app.rekord.api.model) and writes untracked output under build/generated/openapi as a main source root. CI passes -Pcontract.spec in both jobs. HealthResource implements the generated HealthApi. SuccessStatus sets a resource method's 201 or 204 success status. Evidence: PR #11, merged as 68e7585, with CI run 37634668615 green on cf5d5ba; follow-up PR #12 (code-map leaf, DoD #10), merged as 161b143, with CI run 37638599179 green on 4db2b24. Each AC is covered by a named test (see notes). DoD #3, #6 and #9 are N/A, with reasons in the notes. Epic TASK-2 stays open because TASK-2.4 is To Do.
<!-- SECTION:FINAL_SUMMARY:END -->
