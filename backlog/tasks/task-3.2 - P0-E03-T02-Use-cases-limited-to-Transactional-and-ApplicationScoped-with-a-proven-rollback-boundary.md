---
id: TASK-3.2
title: >-
  P0-E03-T02 Use cases limited to Transactional and ApplicationScoped with a
  proven rollback boundary
status: Done
assignee: []
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 22:16'
labels:
  - user-story
  - P0
milestone: m-0
dependencies:
  - TASK-3.1
references:
  - 'docs/rewrite/architecture-conventions.md:433-454'
  - 'docs/rewrite/STATUS.md:321-329'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-3
priority: high
type: feature
ordinal: 302
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want a save that fails halfway to leave nothing behind so that my wedding data never ends up half-written.

UD-15.a allows a use case exactly jakarta.transaction.Transactional and jakarta.enterprise.context.ApplicationScoped. Producing use-case beans with @Produces methods instead is rejected because ArC applies interceptor bindings to managed beans, not to producer results, so the transaction boundary would leave the use case (PIN-AC-0448; architecture-conventions §6.2, line 448, INFERRED there). This ticket proves the boundary at transaction level with a test-only port, since P0-E03 comes before the database; the PostgreSQL row-level rollback (PIN-AC-0452) is P0-E04-T02.

- Covers: UD-15.a, PIN-AC-0448

Plan item `P0-E03-T02` (user-story,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a test-only use case annotated @ApplicationScoped with a single constructor and a @Transactional write method When a @QuarkusTest calls the method Then a transaction is active inside it, read through TransactionSynchronizationRegistry.
- [x] #2 Given that write method whose first port call records a write and whose second port call throws a runtime exception When a @QuarkusTest calls it Then the exception reaches the caller, a registered Synchronization sees the status STATUS_ROLLEDBACK in afterCompletion, and the recorded write is discarded.
- [x] #3 Given a use-case class produced by an @Produces method in application instead of being an @ApplicationScoped bean When the ArchUnit suite runs Then its rule that refuses an @Produces method returning a rekord-usecase type fails, so no use case loses its @Transactional boundary.
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
Close-out evidence (wedding-portal PR #20 https://github.com/Vikteur/wedding-portal/pull/20, head e57783a8cb0016e72ead66518b7133d959846b9f, merged by the user as 9abf5f3a04307d7e994550b234ba46fbf5221aaa; the merge tree equals the head tree).
- CI: run 37694254353 (workflow CI, pull_request) on head e57783a, conclusion success. `./gradlew test integrationTest build` ran :application:test, integrationTest, resourceTest, startupTest and check; every step succeeded. The only skipped step is "Upload test reports", which runs only `if: failure()`. No test, gate or check was disabled, skipped or quarantined.
- AC #1: UseCaseTransactionBoundaryIT (@QuarkusTest, integrationTest) a_transaction_is_active_inside_the_transactional_use_case_method reads STATUS_ACTIVE through TransactionSynchronizationRegistry inside RecordWriteUseCase.save, and STATUS_NO_TRANSACTION before and after the call. the_use_case_is_an_application_scoped_class_bean_with_a_single_constructor checks the bean is ApplicationScoped, of kind CLASS, with one written constructor.
- AC #2: a_failing_second_port_call_reaches_the_caller_and_the_recorded_write_is_rolled_back checks three things: the caller gets the same exception the port threw; the registered interposed Synchronization sees exactly [STATUS_ROLLEDBACK] in afterCompletion; committedWrites is empty. The control test a_successful_call_commits_the_recorded_write sees [STATUS_COMMITTED] and the write kept.
- AC #3: UseCaseRules.A3 is now one CompositeArchRule. Besides the dependency check it refuses an @Produces method that returns or builds a use-case type (new, constructor reference, or a call returning one that is assignable to the produced type) and an @Produces field of a use-case type. Ports in app.rekord.usecase..port.. stay producible. UseCaseRulesTest covers the breaking cases: return type, return through the port interface, constructor reference, static factory, field. It also covers the passing guards: a port producer, Instance.get() of the managed bean, a port producer throwing or reading a usecase-package type.
- PIN-AC-0448 pinned: the_same_use_case_from_a_producer_method_runs_without_a_transaction observes STATUS_NO_TRANSACTION and no completion for the same class produced by an @Produces method (bean kind PRODUCER_METHOD).
- DoD #1: each step's tests were committed alone before the code (test(step n) / feat(step n) commits, red logs step-1..4 in the run's artifacts). A review gap was also handled tests first (b3bf749 then 6be4e37), followed by e322fb0 then 5b1a92c.
- DoD #3: the suite is still A1..A14 (ArchitectureSuiteTest). A3's stored.rules key was re-written on its existing id 16ae27d7-…, so the store keeps 14 entries and all 14 violation files are 0 bytes at the merge commit. archunit.properties is unchanged (allowStoreCreation/allowStoreUpdate=false). FrozenBaselineTest is green in CI.
- DoD #4 N/A: no contract change. The pin stays rekordContractTag=v0.1.0 and no generated file was touched.
- DoD #5: the ticket touches no STOP item (no contract push, migration, auth, encryption/logging, history deletion or production config/secrets). The user's merge of PR #20 as 9abf5f3a records the human review and approval (UD-21.e).
- DoD #6 N/A: no refusal or HTTP operation is built; the change is test-only proof plus an ArchUnit rule.
- DoD #7 N/A: no operation is built. The deviation implemented, UD-15.a, is named in the PR.
- DoD #8: the PR diff has no secret, token, password, access code or personal data. The test data is the literal "first", plus synthetic fixture class names.
- DoD #9 N/A: no schema change and no Flyway migration (the row-level rollback is PIN-AC-0452, P0-E04-T02).
- DoD #10: docs/code-maps/archunit-fitness.md (A3 bullet, guarded by CodeMapLeavesTest.archunit_fitness_records_the_a3_producer_refusal) and docs/memory.md (TASK-3.2 section) were updated in the same PR.
- DoD #11: PR #20 was reviewed and merged into main by the user as 9abf5f3a (UD-21.e).
- application now declares io.quarkus:quarkus-narayana-jta itself; it was already in the deployed artifact through Hibernate ORM.
- Follow-ups (not created): (1) architecture-conventions §6.2 line 448 in this umbrella still marks the @Produces rationale INFERRED; it can now cite UseCaseTransactionBoundaryIT as its PIN(test). (2) The PostgreSQL row-level rollback with real rows remains PIN-AC-0452 in P0-E04-T02.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Use cases are limited to @Transactional + @ApplicationScoped, and the rollback boundary is proven at transaction level with a test-only port (no database). UseCaseTransactionBoundaryIT shows a transaction is active inside the @Transactional method. It also shows that a failing second port call reaches the caller, ends in STATUS_ROLLEDBACK and discards the recorded write, and that the same use case from an @Produces method runs without a transaction, which pins PIN-AC-0448. A3 became a composite rule that refuses @Produces methods and fields returning or building a use-case type, with no new frozen-baseline entry (14 empty store files). Evidence: wedding-portal PR #20, merged by the user as 9abf5f3a04307d7e994550b234ba46fbf5221aaa; CI run 37694254353 green on head e57783a8cb0016e72ead66518b7133d959846b9f.
<!-- SECTION:FINAL_SUMMARY:END -->
