---
id: TASK-4.3
title: P0-E04-T03 Datasource-less test profile for resource tests
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 12:06'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-4.1
references:
  - 'docs/rewrite/architecture-conventions.md:541-628'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-4
priority: high
type: task
ordinal: 403
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want resource tests to start Quarkus without a database so that a web test checks the HTTP layer alone.

PIN-AC-0535 asks for a test profile in which resource tests start without a datasource, schema set-up or Dev Services. If Quarkus cannot start that way, the PIN's own fallback applies: resource tests run against the shared PostgreSQL container of architecture-conventions §8.5, and the outcome is recorded in docs/memory.md.

- Covers: PIN-AC-0535

Plan item `P0-E04-T03` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the resource-test profile When a @QuarkusTest starts with it Then Quarkus starts without a datasource, without schema set-up and without Dev Services, and no container is started.
- [x] #2 Given a resource test in that profile When it calls GET /api/health Then the answer is 200 {"ok":true}.
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
Finalization evidence (PR https://github.com/Vikteur/wedding-portal/pull/6, head f191e746a84d7b503e4952b5a79283b71c965354, merged 2026-10-07T12:04:51Z as 9fc8f4e98d7f92a785b6f32ce969920130619164; the merge tree is identical to the head tree).
- AC #1: ResourceTestProfileIT (@QuarkusTest, @TestProfile(ResourceTestProfile.class), @Tag("resource-test")) runs in the resourceTest task with TESTCONTAINERS_IMAGE_SUBSTITUTOR=app.rekord.application.ContainerTripwire. It asserts the tripwire is armed, the default AgroalDataSource is inactive with no quarkus.datasource.jdbc.url, and Flyway and the Hibernate SessionFactory are inactive. Any container start fails the boot. Falsified locally (commit 3aaba66 body): removing the devservices and datasource.active overrides made the boot fail in ContainerTripwire. ContainerTripwireTest pins the six overrides. Quarkus 3.39.1 boots datasource-less, so the §8.5 shared-container fallback was not used (recorded in docs/memory.md).
- AC #2: HealthResourceIT now carries the profile and the tag and runs in resourceTest: answers_200_with_json_ok_true_to_a_visitor_without_a_session (200 {"ok":true}) and answers_exactly_what_rekord_api_answers.
- DoD #1: the ITs above fail when their criterion breaks (container start -> tripwire fails the boot; an active datasource/Flyway/Hibernate -> assertions fail; a wrong health answer -> HealthResourceIT fails). ResourceTestTaggingTest keeps the tag, the profile and the *IT name together so a test cannot slip past the tripwire.
- DoD #2: CI run 37616044440 (workflow CI, headSha f191e746a84d7b503e4952b5a79283b71c965354) concluded success; the build and image jobs are green. The log shows :application:test, integrationTest, resourceTest, startupTest and check running, and BUILD SUCCESSFUL. The only skipped step is "Upload test reports", which runs only on failure (if: failure()). No test is disabled or quarantined.
- DoD #3 N/A: wedding-portal has no ArchUnit rules or frozen baseline yet (no archunit reference in the merged tree), so the ticket adds none.
- DoD #4 N/A: no contract change; rekord-contract is untouched.
- DoD #5: the ticket touches no STOP item (no contract push, migration, auth/access matrix, personal-data encryption or logging, history deletion, production config or secrets; the main application.properties is unchanged and the profile lives in src/test). The human review is recorded in the PR comment https://github.com/Vikteur/wedding-portal/pull/6#issuecomment-6037486152 ("Approved", Vikteur, 2026-10-07T12:02:43Z, on head f191e746 before the merge).
- DoD #6 N/A: no refusal path is built; test infrastructure only.
- DoD #7: no new operation. The existing health operation keeps the rekord-api parity (HealthResourceIT#answers_exactly_what_rekord_api_answers, green in resourceTest). No UD deviation is implemented.
- DoD #8: the diff adds no secret, token, password, access code or personal data (test classes, build.gradle.kts, docs/memory.md only).
- DoD #9 N/A: no schema change and no migration.
- DoD #10: docs/memory.md gains a "2026-10-07 — TASK-4.3 resource-test profile" section that supersedes the TASK-4.1 bullet "Every @QuarkusTest needs Docker"; BuildLayoutTest#memory_records_the_resource_test_profile guards it. No other document or code-map leaf is outdated.
- DoD #11: PR #6 reviewed (comment above) and merged into main as 9fc8f4e.
Follow-up (not created): the first slice's resource tests should introduce the per-slice AbstractResourceTest base of architecture-conventions §7.3, once @TestProfile inheritance is confirmed on Quarkus 3.39.1.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resource tests now boot Quarkus without a datasource, Flyway, Hibernate or Dev Services, through ResourceTestProfile (a QuarkusTestProfile in src/test). They run in their own resourceTest Gradle task under check, where the ContainerTripwire Testcontainers substitutor makes any container start fail the boot. HealthResourceIT moved to that profile and no longer needs Docker, and docs/memory.md records the decision. Verified by ResourceTestProfileIT, HealthResourceIT, ContainerTripwireTest, ResourceTestTaggingTest and BuildLayoutTest, all green in CI run 37616044440 on head f191e746a84d7b503e4952b5a79283b71c965354 of PR #6, merged as 9fc8f4e98d7f92a785b6f32ce969920130619164. Review and approval: PR comment 6037486152.
<!-- SECTION:FINAL_SUMMARY:END -->
