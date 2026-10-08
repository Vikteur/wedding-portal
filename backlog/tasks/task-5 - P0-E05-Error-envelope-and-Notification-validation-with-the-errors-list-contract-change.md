---
id: TASK-5
title: >-
  P0-E05 Error envelope and Notification validation with the errors-list
  contract change
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - epic
  - P0
milestone: m-0
dependencies:
  - TASK-3
references:
  - 'docs/rewrite/STATUS.md:270-278'
  - 'docs/rewrite/architecture-conventions.md:745-798'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:1-122'
  - rekord-api/src/main/java/app/rekord/error/ErrorCodes.java
  - .claude/skills/validation-notification-result/SKILL.md
  - rekord-contract/components/common.yaml
  - 'docs/rewrite/STATUS.md:394-405'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/backlog-plan/definition-of-done.json
priority: high
ordinal: 500
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want every refusal to carry the error envelope with a contract error code and every validation refusal to name all violations at once so that the apps show me why a request failed.

Goal: one error model for every phase. Scope: the envelope {"detail":{"code","message"}} with codes from the contract's ErrorCode enum per exception family (architecture-conventions §11); framework-raised errors keep the answers rekord-api gives (§11.2), each pinned from rekord-api first; the Notification pattern for all validation (UD-12.a): every rule evaluated, every violation collected, the use case decides once; the 422 VALIDATION_FAILED answer with one message naming every violation and the new optional errors list of {field, code, message} items, an additive contract change that is a STOP item (UD-12.b); field is the snake_case request property path with list indexes or null, code is one of REQUIRED, TOO_SHORT, TOO_LONG, OUT_OF_RANGE, INVALID_FORMAT and INVALID_VALUE, a request that breaks contract constraints lists only those, and a body that is not JSON, a value of the wrong JSON type and an unknown enum value answer 422 VALIDATION_FAILED too (UD-19.d); the Python-only status and code differences, settled in favour of rekord-api and the contract (UX-09, UX-10). Out of scope: the validation rules of each operation (its build ticket), the role-denied 403 body (pinned in P0-E06), and the access matrix of the planner operations (P1-E09). A unique-constraint clash that slips past a domain check is translated in the persistence adapter, inside the port call, into the refusal that check raises, and answers 409 with the code, message and errors list of that check, whatever status a sequential second call gets (409 DUPLICATE_NAME for a RejectedException check, 409 VALIDATION_FAILED with its errors item for a Notification rule whose sequential refusal is 422); no persistence exception crosses a port, and any other persistence failure, a failure raised at commit included, answers 500 UNKNOWN (UD-19.i5, a deliberate deviation from rekord-api's 500 UNKNOWN). The race test of each guarded constraint belongs to the ticket that brings the constraint.

Plan item `P0-E05` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a request body that breaks two validation rules at once When the use case validates it Then the answer is 422 VALIDATION_FAILED whose message names both violations in one sentence and whose errors list holds one {field, code, message} item per violation (deviation UD-12.b and UD-19.d; rekord-api answers a detail with code and message only).
- [ ] #2 Given the contract tag wedding-portal pins at the end of phase 0 When its error schema is read Then it holds the optional errors list next to code and message, merged after a recorded human approval of the contract push, and no other field of the error schema has changed.
- [ ] #3 Given each exception family of architecture-conventions §11.1 When a resource test raises it Then the status and body equal the answer rekord-api gives for that family.
- [ ] #4 Given two calls racing for one unique constraint that a domain check guards, once a RejectedException check answering 409 DUPLICATE_NAME and once a Notification rule answering 422 VALIDATION_FAILED to a sequential duplicate When the losing call's insert reaches the database inside its save port call Then the loser answers 409 DUPLICATE_NAME and 409 VALIDATION_FAILED with that check's message and errors list in turn, naming no table or constraint, and a clash on a constraint no domain check guards answers 500 UNKNOWN "Something went wrong at our end." (deviation UD-19.i5; rekord-api answers 500 UNKNOWN to all three).
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
