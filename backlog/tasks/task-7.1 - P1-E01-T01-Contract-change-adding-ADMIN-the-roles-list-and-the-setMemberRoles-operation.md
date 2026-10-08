---
id: TASK-7.1
title: >-
  P1-E01-T01 Contract change adding ADMIN, the roles list and the setMemberRoles
  operation
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - technical
  - P1
  - stop-contract-push
  - stop-auth-access
milestone: m-1
dependencies: []
references:
  - 'rekord-contract/components/identity.yaml:14-81'
  - 'rekord-contract/components/identity.yaml:148-170'
  - 'rekord-contract/openapi.yaml:170-176'
  - 'rekord-contract/paths/planner.yaml:783-857'
  - 'docs/rewrite/STATUS.md:298-320'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: task
ordinal: 10101
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a maintainer of rekord-contract, I want the role model of UD-14 in the contract hub before any code uses it so that wedding-portal and both apps regenerate from one agreed shape.

The contract-first step for UD-14 (P4 of the constitution: the spec changes and is tagged before app code). Today the contract knows two roles (identity.yaml:14-25), Me and UserAccount carry one role each (identity.yaml:38-81), and no operation grants or revokes a role; the member operations sit under /org/members (openapi.yaml:170-176, paths/planner.yaml:783-857). The change adds ADMIN to Role, an optional roles array to Me and UserAccount, lets InviteCreate.role carry ADMIN, and adds the operation setMemberRoles. Me.role and UserAccount.role keep their meaning for today's apps: PLANNER when the member holds ADMIN or PLANNER, DJ otherwise (UD-14.f), so neither field ever carries ADMIN. setMemberRoles is not a catalog operation; it is built by P1-E02-T05. The push to rekord-contract is a STOP item: the ticket prepares the change, a human approves it, and only then is it merged and tagged. The user approved this shape (UD-19.g); the push itself still waits for the STOP approval.

- STOP (human approval in the pull request): contract-push, auth-access
- Covers: UD-19.g

Plan item `P1-E01-T01` (technical,P1,stop-contract-push,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given Role in rekord-contract/components/identity.yaml with the values PLANNER and DJ When the change is merged after a recorded human approval Then Role holds ADMIN, PLANNER and DJ, Me and UserAccount each gain an optional property roles (an array of Role), InviteCreate.role accepts ADMIN, and no other property of these schemas changes (UD-14.f).
- [ ] #2 Given the descriptions of Me.role, UserAccount.role and the new roles arrays When the change is read Then Me.role and UserAccount.role state that the value is PLANNER when the member holds ADMIN or PLANNER and DJ otherwise, and roles states that it lists every ACTIVE role of the member in the business sorted by name (ADMIN, DJ, PLANNER).
- [ ] #3 Given the paths file of the member operations When the change is read Then it holds the operation setMemberRoles as PUT /org/members/{userId}/roles with the security of the other member operations, a required body with a required array roles of 1 to 3 unique Role values, the answer 200 UserList, and error answers 400 LAST_ADMIN, 403 FORBIDDEN, 404 NO_USER and 422 VALIDATION_FAILED through the common Error response (UD-14.d).
- [ ] #4 Given the description of setMemberRoles When it is read Then it states that only an admin is allowed to call it, that the body replaces the member's whole role set, that an empty set is refused with 422, that taking ADMIN from the last active admin is refused with 400 LAST_ADMIN, and that a change of the role set ends every session of the member (UD-14.a, UD-14.e, UD-19.g).
- [ ] #5 Given the pull request with the change When the contract CI of P0-E02 runs Then the breaking-change job reports no removed or renamed operation, property or enum value and no newly required property, and the merge commit gets a tag that wedding-portal then pins.
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
