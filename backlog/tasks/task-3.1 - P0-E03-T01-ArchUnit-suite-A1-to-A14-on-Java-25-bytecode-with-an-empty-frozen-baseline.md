---
id: TASK-3.1
title: >-
  P0-E03-T01 ArchUnit suite A1 to A14 on Java 25 bytecode with an empty frozen
  baseline
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 16:48'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-1.1
  - TASK-2.3
references:
  - 'docs/rewrite/architecture-conventions.md:199-231'
  - '.claude/hooks/hooks.env:23-29'
  - 'docs/rewrite/architecture-conventions.md:490-499'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-3
priority: high
type: task
ordinal: 301
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the layer rules checked by ArchUnit in every CI run so that a framework import in the domain turns the build red without a reviewer.

architecture-conventions §3 defines the rules A1 to A13 in application/src/test/java/app/rekord/architecture, run inside the ordinary test task, with an empty frozen baseline and the one planned exception of §6.2 written into A3 (CONV-5, CT-11). PIN-AC-0230 asks whether ArchUnit reads Java 25 class files; PIN-17-0424 and PIN-17-0723 ask that one framework import in a domain class turns CI red. ARCH_FITNESS_CMD stays unset because CI gives the answer (§3; .claude/hooks/hooks.env:23-29). The suite also guards the success-status convention of architecture-conventions §7.1 as rule A14, which §3 does not list: only the helper in app.rekord.adapter.web.shared, built in P0-E02-T03, sets a response status.

- Covers: CONV-5, PIN-AC-0230, PIN-17-0424, PIN-17-0723

Plan item `P0-E03-T01` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given archunit-junit5 as a test dependency of application When CI runs the test task Then the suite in application/src/test/java/app/rekord/architecture checks the rules A1 to A13 of architecture-conventions §3 and rule A14 of this ticket against the classes of all six modules compiled at release 25.
- [x] #2 Given a class in rekord-domain that imports a jakarta or io.quarkus type When CI runs Then rule A1 fails and the run on that commit SHA is red.
- [x] #3 Given the frozen-violation store When the suite runs on the skeleton Then the store holds no entry, and a new violation fails the build instead of being added to the store.
- [x] #4 Given rule A3 When it checks rekord-usecase Then the only framework types it allows are jakarta.transaction.Transactional and jakarta.enterprise.context.ApplicationScoped, written into the rule as the CT-11 exception.
- [x] #5 Given a resource class in rekord-adapter that does not implement a generated app.rekord.api interface When the suite runs Then rule A11 fails.
- [x] #6 Given a main-source class with a field annotated @Inject When the suite runs Then rule A9 fails.
- [x] #7 Given a class in app.rekord.adapter.web other than the helper in app.rekord.adapter.web.shared that sets a response status When the ArchUnit suite runs Then rule A14 (only app.rekord.adapter.web.shared sets a response status) fails.
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [ ] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [ ] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [ ] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [ ] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [ ] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Finalize, done by hand: the build-feature run f8b058eb failed at its ready node because PR #15 was auto-merged while still a draft, so the finalize and close-out nodes never ran. The branch and worktree were then removed by hand with archon complete.

AC #4 deviation: rule A3 also allows jakarta.transaction.Transactional$TxType, because ArchUnit reports the default member of @Transactional as a dependency. It is in the code-map leaf docs/code-maps/archunit-fitness.md, not in the rule's because-text.

DoD N/A:
- #4: no contract change.
- #5: no STOP item touched.
- #6 and #7: no operation built.
- #9: no schema change.

DoD #10: the docs/memory.md placeholder for PIN-AC-0230 was filled in by follow-up PR #17 (test 99654e3, docs efcf059, CI run 37654140455 green, merged as 7c96a9c).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
wedding-portal PR #15 adds the ArchUnit 1.5.1 suite A1 to A14, which reads the Java 25 class files of all six modules through ProductionClasses. It went green in CI run 37652403500 on fd40198 and was merged as 5f7f1df.

- The rules are split across DomainRules, UseCaseRules, LayerRules, AdapterRules and ProductionCodeRules.
- A3 allows only Transactional and ApplicationScoped (CT-11), plus Transactional$TxType.
- FrozenBaselineTest checks the frozen store under application/src/test/archunit_store. The store has no violations, and archunit.properties forbids creating or updating it, so a new violation fails the build.
- A focused test proves each acceptance criterion: A1 framework import in the domain, A9 field injection, A11 resource without a generated interface, A14 response status outside adapter.web.shared.
- The code-map leaf docs/code-maps/archunit-fitness.md is updated.
- Follow-up PR #17 (merged as 7c96a9c) records the CI run that settled PIN-AC-0230 in docs/memory.md.

This unblocks TASK-3.2 and TASK-5.1.
<!-- SECTION:FINAL_SUMMARY:END -->
