---
id: TASK-4.4
title: P0-E04-T04 WireMock 3 gateway-test harness on a dynamic port
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 12:35'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-1.1
references:
  - 'docs/rewrite/analysis/18-framework-code-rulebook.md:552'
  - .claude/skills/wiremock-gateway-stubs/SKILL.md
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-4
priority: high
type: task
ordinal: 404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want gateway tests to run against WireMock 3 on a dynamic port so that outbound calls are tested without the real services.

PIN-18-0552 (18 C-25) settles the WireMock coordinates on org.wiremock (WireMock 3) instead of com.github.tomakehurst; PIN-AC-0655 asks that one gateway test with the extension passes in CI, on a dynamic port and reset between tests (FW-C-53).

- Covers: PIN-18-0552, PIN-AC-0655

Plan item `P0-E04-T04` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the test dependencies When the build resolves them Then WireMock comes from org.wiremock at a 3.x version pinned in gradle/libs.versions.toml, and no com.github.tomakehurst artifact is on any classpath.
- [x] #2 Given a gateway test When it starts Then WireMock listens on a dynamic port and the gateway's base-URL setting points at that port for the test.
- [x] #3 Given two gateway tests in one run When the second starts Then the stubs and recorded requests of the first are reset.
- [x] #4 Given a test-only gateway in rekord-gateway calling a stubbed endpoint When its test runs in CI Then it passes and the stub received exactly one request.
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
Finalization evidence (PR https://github.com/Vikteur/wedding-portal/pull/7, head 7b0ad4a88ae0dc400360d22b5f02c8fe88a9eae1, merged 2026-10-07T12:34:14Z as 9b7aab1321aca3a2a2507860bbc3ba8469ff8af3; the merge tree equals the head tree):
- CI run 37621014223 (workflow CI, pull_request) on head 7b0ad4a: success. Its build job ran ./gradlew test integrationTest build --stacktrace (BUILD SUCCESSFUL), including :rekord-gateway:integrationTest and legacyWireMockCheck in all six modules (application, logging, rekord-adapter, rekord-domain, rekord-gateway, rekord-usecase).
- AC #1: gradle/libs.versions.toml pins org.wiremock:wiremock-standalone 3.13.2. Tests: BuildLayoutTest.wiremock_3_is_pinned_in_the_catalog_from_org_wiremock, no_build_file_names_a_com_github_tomakehurst_artifact, legacy_wiremock_check_resolves_every_classpath_under_check; the legacyWireMockCheck task under check (mutation by hand: adding com.github.tomakehurst:wiremock-jre8 made it fail); ProbeStatusGatewayIT.wiremock_comes_from_org_wiremock_3_and_no_legacy_jar_is_on_the_classpath.
- AC #2: ProbeStatusGatewayIT.wiremock_listens_on_a_dynamic_port_and_the_gateway_points_at_it.
- AC #3: WireMockResetBetweenTestsIT (two ordered methods share one static server, resetOnEachTest(true); the second asserts no stub and no serve event).
- AC #4: ProbeStatusGatewayIT.the_gateway_calls_the_stubbed_endpoint_once (verify exactly(1) plus getAllServeEvents size 1), with the test-only ProbeStatusGateway in rekord-gateway/src/test; passed in CI run 37621014223.
- DoD #1: each AC above has a test that fails when it is broken (mutations done by hand: fixed port 8080, two calls, reset off, legacy dependency).
- DoD #2: CI run 37621014223 green on 7b0ad4a. No test is @Disabled or skipped. rekord-gateway sets failOnNoDiscoveredTests = false on its test task only, because all its tests are ITs and run in integrationTest. BuildLayoutTest.only_the_all_it_gateway_module_allows_an_empty_fast_test_set keeps the guard on in every other module. This does not skip a test. The image job's -x test -x integrationTest is the existing Docker image build. The build job of the same run executes those tasks.
- DoD #3 N/A: wedding-portal has no ArchUnit rules or frozen baseline yet (no archunit reference in the merged tree 9b7aab1); this ticket adds none.
- DoD #4 N/A: no contract change.
- DoD #5: no STOP item touched (test-scope dependencies, a build verification task, test-only classes and a memory note). The human review is recorded in PR comment https://github.com/Vikteur/wedding-portal/pull/7#issuecomment-6037903612 ("Approved", by Vikteur, 2026-10-07T12:28:17Z, on head 7b0ad4a before the merge).
- DoD #6 N/A: no refusal path or endpoint built.
- DoD #7 N/A: no operation built; the probe gateway is test-only.
- DoD #8: stub bodies are synthetic ({"status":"up"}); the probe gateway logs nothing and sends no identifiers or credentials.
- DoD #9 N/A: no schema change.
- DoD #10: docs/memory.md section '2026-10-07 — TASK-4.4 WireMock harness' (guarded by BuildLayoutTest.memory_records_the_wiremock_harness).
- DoD #11: reviewed (PR comment above) and merged into main as 9b7aab1.
- The memory note says the run id and SHA are filled in after CI. They are CI run 37621014223 on 7b0ad4a; the close-out node may record them in docs/memory.md.
- Follow-ups (not created): remove the test-only ProbeStatusGateway once the first real gateway (Spotify) has its own WireMock test; DoD #3 is checked once ArchUnit arrives with TASK-3.1.
- Parent TASK-4 stays open: TASK-4.2 and TASK-4.5 are still To Do.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a WireMock 3 gateway-test harness to rekord-gateway. org.wiremock:wiremock-standalone 3.13.2 is pinned in the catalog. A legacyWireMockCheck task under check fails if any classpath resolves com.github.tomakehurst. A test-only ProbeStatusGateway is tested with ProbeStatusGatewayIT (dynamic port, base URL, exactly one request, 5xx failure, artifact origin), and WireMockResetBetweenTestsIT tests the reset between tests. Evidence: PR #7 (https://github.com/Vikteur/wedding-portal/pull/7) merged as 9b7aab1321aca3a2a2507860bbc3ba8469ff8af3; CI run 37621014223 green on head 7b0ad4a88ae0dc400360d22b5f02c8fe88a9eae1; reviewed in PR comment 6037903612 by Vikteur.
<!-- SECTION:FINAL_SUMMARY:END -->
