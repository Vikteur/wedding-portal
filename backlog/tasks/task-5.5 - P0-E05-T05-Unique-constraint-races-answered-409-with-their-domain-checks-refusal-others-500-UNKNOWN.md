---
id: TASK-5.5
title: >-
  P0-E05-T05 Unique-constraint races answered 409 with their domain check's
  refusal, others 500 UNKNOWN
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
milestone: m-0
dependencies:
  - TASK-5.2
  - TASK-4
references:
  - 'docs/rewrite/architecture-conventions.md:566-576'
  - 'docs/rewrite/analysis/08-api-behaviour.md:108'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:107'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:109-121'
  - 'docs/rewrite/STATUS.md:429-431'
  - 'docs/rewrite/analysis/41-domain-model-and-glossary.md:1608'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: task
ordinal: 505
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want a unique-constraint clash that slips past a domain check answered 409 with that check's code, message and errors list and every other database failure answered 500 UNKNOWN so that a race gives the caller the readable refusal of the check and no schema detail reaches a client.

UD-19.i5 (RISK-26): a unique-constraint clash that slips past a domain check answers with the same code and message as that domain check, instead of the 500 UNKNOWN rekord-api answers (docs/rewrite/analysis/08-api-behaviour.md:108; 41 D-3 at docs/rewrite/analysis/41-domain-model-and-glossary.md:1608 is superseded on this point). UD-19.i5 states the status 409 for every such race, so a race answers 409 with the code, message and errors list of the check: for a check that throws a RejectedException, such as the library-name check, that is 409 DUPLICATE_NAME, the bytes of a sequential second call; for a Notification rule, such as the open-invite check of createInvite (P1-E02-T06), it is 409 VALIDATION_FAILED with the check's message and errors list, which differs from the 422 of a sequential second call in its status only and keeps the Error schema of the contract. This 409 is the one status that does not come from ErrorStatusTable (P0-E05-T01). architecture-conventions §8.2: the persistence adapter translates the violation of a named unique constraint, by constraint name, into the same typed exception the domain check throws, so no persistence exception crosses a port. The translation can only happen inside the port call: a violation raised later, at the commit of the use-case transaction, is raised by the transaction boundary after the port call has returned, never reaches the adapter and is answered by the catch-all of P0-E05-T02. Every other persistence failure (a clash on a constraint no domain check guards, an optimistic-lock failure, a pessimistic-lock failure) answers 500 UNKNOWN. BR-DM-41: only jakarta.validation.ConstraintViolationException maps to 422. PIN-AC-0574 asks one test per mapped constraint. This ticket builds the mechanism only; the constraint map of app.rekord.adapter.persistence holds no entry in phase 0, and each ticket that brings a guarded unique constraint adds its entry and its race test and covers UD-19.i5 for it: the four phase-1 races of P1-E09-T07 (RISK-26), the invite race of createInvite among them, each answering 409 VALIDATION_FAILED, and the library-name races of createLibrary and renameLibrary in phase 3 (P3-E09-T02, criteria 1 and 2). This is the one race policy of every phase. A ticket keeps two simultaneous calls from clashing at all only by one of three means, each giving both calls the answers of the same two calls made one after the other: a row lock taken before the domain check (SELECT … FOR UPDATE on the row the write belongs to, such as a song_lists row or a libraries row; architecture-conventions §8.1, FW-C-39), an in-process lock, or INSERT … ON CONFLICT for a write whose two calls store the same row.

- Covers: BR-DM-41, PIN-AC-0574

Plan item `P0-E05-T05` (technical,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a test-only table test_names with the unique constraint uq_test_names_name, mapped to a test-only domain check that refuses an existing name with a RejectedException DUPLICATE_NAME "That name is taken.", and a test latch that holds two calls for the name alpha after that check until both pass it When both calls insert alpha through the save port, which writes to the database before it returns Then the losing call's violation is translated inside its port call and that call answers 409 {"detail":{"code":"DUPLICATE_NAME","message":"That name is taken."}}, the bytes of a sequential second call, naming no table, column or constraint (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
- [ ] #2 Given test_names with the unique constraint uq_test_names_slug, mapped to a test-only Notification rule that refuses an existing slug with the item {"field":"slug","code":"INVALID_VALUE","message":"is already used"}, and the latch of criterion 1 holding two calls for the slug beta When both calls insert beta through the save port Then the losing call answers 409 {"detail":{"code":"VALIDATION_FAILED","message":"slug is already used","errors":[{"field":"slug","code":"INVALID_VALUE","message":"is already used"}]}}, the body a sequential second call gets with 422 (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
- [ ] #3 Given the table test_names with a third unique constraint uq_test_names_code that no map entry names When two calls insert the code c-1 through the save port Then the second answers 500 with the body {"detail":{"code":"UNKNOWN","message":"Something went wrong at our end."}} and no table, column or constraint name.
- [ ] #4 Given the table test_names with a fourth unique constraint uq_test_names_alias that is declared DEFERRABLE INITIALLY DEFERRED and that no map entry names, so its violation is raised at the commit of the use-case transaction after every port call has returned When two calls store the alias gamma one after the other Then the second answers 500 with the body {"detail":{"code":"UNKNOWN","message":"Something went wrong at our end."}} from the catch-all of P0-E05-T02, the body holds no table, column or constraint name, and the first call's row is the only row with the alias gamma.
- [ ] #5 Given an unmapped constraint violation, an optimistic-lock failure and a pessimistic-lock failure raised inside a port call of the persistence adapter When each one propagates through the port Then the exception the use case receives is of no jakarta.persistence, org.hibernate or java.sql type, and the answer is 500 UNKNOWN with the message "Something went wrong at our end."
- [ ] #6 Given a jakarta.validation.ConstraintViolationException raised at the web or use-case boundary When it is answered Then the status is 422 VALIDATION_FAILED (BR-DM-41), while an org.hibernate.exception.ConstraintViolationException on a constraint no map entry names answers 500 UNKNOWN.
- [ ] #7 Given the map from database constraint name to domain refusal in app.rekord.adapter.persistence, holding no entry in phase 0 When the integrationTest task runs Then a guard test fails the build for any name in the map that has no Testcontainers test forcing that violation and asserting the status 409 and the code, message and errors list of its domain check, and with no entry the guard test passes.
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
