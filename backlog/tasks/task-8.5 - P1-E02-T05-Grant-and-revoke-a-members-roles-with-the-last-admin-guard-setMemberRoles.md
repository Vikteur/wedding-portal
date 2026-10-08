---
id: TASK-8.5
title: >-
  P1-E02-T05 Grant and revoke a member's roles with the last-admin guard
  (setMemberRoles)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-contract-push
milestone: m-1
dependencies:
  - TASK-7.1
  - TASK-8.4
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:59-81'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:149-172'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:50-59'
  - 'docs/rewrite/STATUS.md:298-312'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10205
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to set the roles a member holds so that a DJ who starts planning or a planner who takes over the business gets the right rights.

Implements setMemberRoles, the operation the contract change of P1-E01-T01 adds (UD-14.d); it is not a catalog operation of rekord-api and has no oracle. The body names the member's whole role set: memberships for roles that are new are created with the status of the account, memberships for roles that are left out are deleted, and the answer is the whole member list. Only an admin calls it; a planner without ADMIN gets 403 FORBIDDEN "This is not yours to open." and a DJ gets 403. An empty set is refused with 422 (UD-14.a). New message text (no oracle): LAST_ADMIN "The last admin cannot lose the ADMIN role — there would be nobody left to invite one back.". Taking ADMIN from the last active admin is refused with 400 LAST_ADMIN (UD-14.e). Roles are snapshotted into a session at sign-in (BR-ID-13); a change of the role set therefore ends every session of the member, the caller's own session included when admins change their own set, as a status change does (AccountsResource.java:66-72; UD-19.g); a call that leaves the set unchanged ends no session. The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- STOP (human approval in the pull request): auth-access, contract-push
- Covers: UD-14.a, UD-14.e, UD-19.g

Plan item `P1-E02-T05` (user-story,P1,stop-auth-access,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ holding only DJ and signed in on two devices When an admin calls setMemberRoles for the DJ with roles [DJ, PLANNER] Then the answer is 200 with the member list showing the member with role PLANNER and roles [DJ, PLANNER], both devices' sessions answer 401 NOT_SIGNED_IN, and after a new login getMe shows roles [DJ, PLANNER] (UD-19.g).
- [ ] #2 Given a member holding ADMIN and PLANNER and signed in on one device When an admin calls setMemberRoles for the member with roles [PLANNER] while another active admin exists Then the answer is 200, the member holds only PLANNER and the member's session answers 401 NOT_SIGNED_IN on getMe (UD-19.g).
- [ ] #3 Given a member holding only DJ When an admin calls setMemberRoles for the member with roles [] Then the answer is 422 VALIDATION_FAILED with one errors item {field roles, code TOO_SHORT} and the member still holds DJ (UD-14.a, UD-19.d1).
- [ ] #4 Given a business whose only active admin is the caller When the caller calls setMemberRoles for themselves with roles [PLANNER] Then the answer is 400 LAST_ADMIN "The last admin cannot lose the ADMIN role — there would be nobody left to invite one back." and the caller still holds ADMIN and stays signed in (UD-14.e).
- [ ] #5 Given a member whose role set is [DJ], signed in on one device When an admin calls setMemberRoles for the member with roles [DJ] Then the answer is 200, no membership row changes and the member's session still answers 200 on getMe (UD-19.g).
- [ ] #6 Given a DISABLED member holding DJ When an admin grants PLANNER with setMemberRoles Then the new PLANNER membership is DISABLED, like the member's other membership.
- [ ] #7 Given a planner without ADMIN When the planner calls setMemberRoles for another member Then the answer is 403 FORBIDDEN "This is not yours to open." and no membership changes (UD-14.d).
- [ ] #8 Given a member holding only DJ When the member calls setMemberRoles for another member Then the answer is 403 and no membership changes (UD-14.d).
- [ ] #9 Given an unknown member id, a removed member and a member of another business When an admin calls setMemberRoles for each with roles [DJ] Then each answer is 404 NO_USER "There is no such account." and no membership changes.
- [ ] #10 Given a body listing DJ twice When an admin calls setMemberRoles with it Then the member holds DJ once, because roles is a set: the generated jaxrs-spec model holds a uniqueItems array as a java.util.Set (openapi-generator 7.25.0, rekord-api/pom.xml:180-193).
- [ ] #11 Given two active admins A and B, A signed in on one device When A calls setMemberRoles for A with roles [ADMIN, DJ] Then the answer is 200, A holds ADMIN and DJ, and A's session then answers 401 NOT_SIGNED_IN on getMe (UD-19.g).
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
