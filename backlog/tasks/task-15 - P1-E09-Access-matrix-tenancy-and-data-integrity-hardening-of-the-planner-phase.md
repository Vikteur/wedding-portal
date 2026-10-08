---
id: TASK-15
title: >-
  P1-E09 Access matrix, tenancy and data-integrity hardening of the planner
  phase
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-11
  - TASK-12
  - TASK-13
  - TASK-14
references:
  - 'docs/rewrite/STATUS.md:298-302'
  - 'docs/rewrite/business-analysis.md:1406-1473'
  - 'docs/rewrite/analysis/41-domain-model-and-glossary.md:1209-1279'
  - 'docs/rewrite/architecture-conventions.md:732-744'
  - 'docs/rewrite/analysis/40-api-compatibility-matrix.md:39-81'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10900
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want every planner operation to refuse the ids of another business and every role to reach exactly the operations the access matrix grants so that no business sees or changes another business's data.

Goal: the cross-cutting rules of phase 1 hold over all its operations. Scope: an admin reaching everything a planner and a DJ reach on every wedding of the business without a team-slot assignment (UD-14.b); the access matrix of the 45 phase-1 operations asserted in one test; ids of another business in the path answered with the operation's not-found refusal (BR-DM-07); foreign-key ids from request bodies checked against the business (RISK-01, BR-DM-29); labels resolved only inside the business (RISK-10); the role-denied 403 answered with the error envelope (RISK-18, UD-19.i4); the error envelope on malformed requests; a unique-constraint clash that slips past a domain check answered 409 with that check's code and message (RISK-26, UD-19.i5); last write wins with no version check (RISK-27, UD-19.h); no GET operation changes data (RISK-12, UD-19.h); timestamps and ids; a daily cleanup job with the retention periods of UD-19.j (RISK-35); a minimal audit_log of sign-in, member, invitation and portal events (UD-19.k). Each build ticket already states its own refusals; this epic holds the rules that span operations. Out of scope: the importer risks, superseded by UD-13.

Plan item `P1-E09` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given two businesses each with a wedding, a vendor and a member When a planner of the first business calls any phase-1 operation with an id of the second business in the path Then the answer is the not-found refusal of that operation and no row of the second business changes.
- [ ] #2 Given two businesses When a planner of the first business sends an id of the second business in a request body Then a task's wedding_id answers 404 NO_WEDDING "There is no such wedding.", every other body id answers 422 VALIDATION_FAILED with the errors item {field <the field sent>, code INVALID_VALUE} (UD-19.i2), and no row is stored or changed.
- [ ] #3 Given the access matrix of the phase-1 operations and the new role operation When the access-matrix test calls each operation as a visitor, a portal session, a DJ, a planner and an admin without a team-slot assignment Then each answer matches the matrix, the admin reaches every operation the planner reaches on every wedding of the business, and every role-denied 403 is the envelope of P1-E09-T01 (UD-19.i4).
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
