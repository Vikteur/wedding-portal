---
id: TASK-5.4
title: >-
  P0-E05-T04 Optional errors list in the error schema, merged and tagged in
  rekord-contract
status: To Do
assignee: []
created_date: '2026-10-07 05:46'
labels:
  - technical
  - P0
  - stop-contract-push
milestone: m-0
dependencies:
  - TASK-5.3
  - TASK-2.4
references:
  - rekord-contract/components/common.yaml
  - 'docs/rewrite/STATUS.md:270-278'
  - 'rekord-contract/dist/openapi.yaml:2039'
  - 'docs/rewrite/STATUS.md:394-408'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-5
priority: high
type: task
ordinal: 504
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want the optional errors list added to the error schema through an approved and tagged contract change so that wedding-portal can send every violation without breaking an app.

UD-12.b: a refusal is 422 VALIDATION_FAILED whose message names every violation in one sentence, plus a new optional errors list. UD-19.d fixes each item as {field, code, message}: field is the request property path in the contract's snake_case with list indexes (lines[2].name), or null for a rule that checks no single property; code is one of REQUIRED, TOO_SHORT, TOO_LONG, OUT_OF_RANGE, INVALID_FORMAT and INVALID_VALUE. Adding a field to the error schema (rekord-contract/components/common.yaml) is a contract change and a STOP item (contract push); it is additive, so the unchanged apps keep reading code and message, and under UD-19.e it raises the minor part of the tag. The item codes are a new list next to the ErrorCode enum, which this change leaves as it is. Filling the list is P0-E05-T06.

- STOP (human approval in the pull request): contract-push
- Covers: UD-12.b, UD-19.d1

Plan item `P0-E05-T04` (technical,P0,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the error schema in rekord-contract/components/common.yaml When the change is merged after a recorded human approval Then the error detail holds code, message and a new optional errors array whose items each require field, code and message, field is a string that is allowed to be null, code is a string enum of exactly REQUIRED, TOO_SHORT, TOO_LONG, OUT_OF_RANGE, INVALID_FORMAT and INVALID_VALUE, message is a string, and no other field of the error schema changes (UD-19.d).
- [ ] #2 Given the pull request with that change and no semver label or the label semver:minor When the contract CI runs and the pull request is merged Then the breaking-change job reports no ERR-level change, and the pull request raises info.version by the minor part of the highest v<major>.<minor>.<patch> tag, which is v0.1.0 or later from the first merge after P0-E02-T02 is in place, and the merge commit gets the tag v<info.version> (UD-19.e, UD-20.a, UD-20.b).
- [ ] #3 Given wedding-portal pinned to that tag When openApiGenerate runs Then the generated error detail carries the optional errors list with the six item codes, and the generated ErrorCode enum holds the same constants it held at the tag wedding-portal pinned before this change.
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
