---
id: TASK-3
title: >-
  P0-E03 Architecture rules, use-case transactions and the agent framework for
  Quarkus
status: Done
assignee: []
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 22:17'
labels:
  - epic
  - P0
milestone: m-0
dependencies:
  - TASK-2
references:
  - 'docs/rewrite/STATUS.md:321-323'
  - 'docs/rewrite/architecture-conventions.md:199-231'
  - 'docs/rewrite/architecture-conventions.md:403-462'
  - 'docs/rewrite/architecture-conventions.md:1109-1144'
  - docs/rewrite/analysis/17-framework-process-rulebook.md
  - docs/rewrite/analysis/18-framework-code-rulebook.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want ArchUnit rules, exactly two framework annotations in use cases and Quarkus-ready skills, rules and hooks so that the layer boundaries hold without reviewer judgment.

Goal: the architecture invariants are defended by CI from the first commit. Scope: the ArchUnit suite with the rules A1 to A13 of architecture-conventions §3, rule A14 for the success-status convention of §7.1 and an empty frozen baseline (CONV-5); ArchUnit reading Java 25 bytecode; use cases allowed exactly jakarta.transaction.Transactional and jakarta.enterprise.context.ApplicationScoped and nothing else from the framework (UD-15.a), with the rollback test of §6.2; the framework's skill bodies, code-map leaves, rule bodies and hooks made Quarkus-ready (CONV-1 to CONV-4, architecture-conventions §16.2 items 1 to 7). Out of scope: §16.2 item 8 and CONV-8 (side-by-side checks with rekord-api on one database), superseded by UD-13; the hook PIN items PIN-17-0755 and PIN-17-0639 are settled by P0-E03-T04, and the PIN items that describe the umbrella workstation (PIN-17-0342, PIN-17-0630, PIN-17-0768) are disposed of as not-applicable in this file.

Plan item `P0-E03` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given a class in rekord-domain that imports a Quarkus or Jakarta type When CI runs Then rule A1 of the ArchUnit suite (the domain is framework-free) fails the build and the frozen baseline holds no entry.
- [x] #2 Given a use-case class that uses a framework type other than jakarta.transaction.Transactional and jakarta.enterprise.context.ApplicationScoped When CI runs Then rule A3 of the ArchUnit suite (the use-case allow-list of UD-15.a) fails the build.
- [x] #3 Given a use case whose second port call throws after its first port call recorded a write through a test-only port When a @QuarkusTest calls it Then the transaction completes as rolled back and the write is not kept, and the same proof with a PostgreSQL row follows in P0-E04.
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
Epic close-out: all four sub-tickets are Done (TASK-3.1, TASK-3.2, TASK-3.3, TASK-3.4); each was closed on its own evidence, recorded in that ticket.
- AC #1: rule A1 (DomainRules) fails a domain class that imports a Jakarta/Quarkus type. DomainRulesTest and FrozenBaselineTest.a_new_violation_of_a_stored_rule_fails_and_leaves_the_store_unchanged (an InjectedThing in app.rekord.domain) prove it. TASK-3.1: wedding-portal PR #15, CI run 37652403500 green on fd40198, merged as 5f7f1df. Still green in CI run 37694254353 on e57783a (PR #20). The store holds 14 empty violation files.
- AC #2: rule A3 (UseCaseRules) fails a use case that uses another framework type. The UseCaseRulesTest cases another_jakarta_transaction_type_breaks_a3, another_cdi_type_breaks_a3 and a_use_case_depending_on_a_framework_type_breaks_a3 prove it (TASK-3.1, PR #15). TASK-3.2 (PR #20) extended A3 to refuse @Produces use cases.
- AC #3: UseCaseTransactionBoundaryIT (TASK-3.2, wedding-portal PR #20, merged by the user as 9abf5f3a04307d7e994550b234ba46fbf5221aaa, CI run 37694254353 green on e57783a8cb0016e72ead66518b7133d959846b9f) proves it. A failing second port call ends in STATUS_ROLLEDBACK seen by a registered Synchronization, and the recorded write is not kept. The PostgreSQL-row proof follows in P0-E04-T02 (PIN-AC-0452).
- DoD #1-#3, #8, #10, #11: met per sub-ticket. Each AC has a failing-when-broken test, the CI runs above are green by SHA with nothing skipped, the frozen baseline holds no entry, docs/code-map leaves were updated in each PR, and every PR was reviewed and merged by the user (wedding-portal #10, #13, #15, #17, #20; weddingapp #36, #37, #38).
- DoD #4 N/A: no contract change in this epic (pin stays v0.1.0).
- DoD #5: no STOP item touched by any sub-ticket. The user's merges record the review and approval (UD-21.e), the last being PR #20 as 9abf5f3a.
- DoD #6 N/A: no refusal or HTTP operation built in this epic.
- DoD #7 N/A: no operation built; the deviation implemented is UD-15.a, named in PR #20.
- DoD #9 N/A: no schema change or Flyway migration in this epic.
- Follow-ups (not created): architecture-conventions §6.2 line 448 can cite UseCaseTransactionBoundaryIT instead of INFERRED. The row-level rollback is PIN-AC-0452 in P0-E04-T02.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
P0-E03 is complete. Four tickets delivered it: the ArchUnit suite A1-A14 on Java 25 bytecode with an empty frozen baseline (TASK-3.1, PR #15, CI 37652403500 on fd40198); use cases limited to @Transactional + @ApplicationScoped, with A3 refusing @Produces use cases and the rollback boundary proven by UseCaseTransactionBoundaryIT (TASK-3.2, PR #20 merged by the user as 9abf5f3a04307d7e994550b234ba46fbf5221aaa, CI 37694254353 green on e57783a8cb0016e72ead66518b7133d959846b9f); Quarkus-ready skills, code maps and rules (TASK-3.3); and effective governance hooks (TASK-3.4). The PostgreSQL row-level rollback follows in P0-E04-T02.
<!-- SECTION:FINAL_SUMMARY:END -->
