---
id: TASK-8
title: 'P1-E02 Members, invites and role management with the last-admin guard'
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-7
references:
  - 'docs/rewrite/STATUS.md:298-317'
  - rekord-api/src/main/java/app/rekord/account/AccountsResource.java
  - rekord-api/src/main/java/app/rekord/account/InviteService.java
  - rekord-contract/components/identity.yaml
  - docs/rewrite/analysis/10-identity-access.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to invite planners, DJs and admins, grant and revoke roles and disable or remove members while one active admin always remains so that the business controls who works in it.

Goal: people management under UD-14. Scope: the operations listMembers, updateMember, deleteMember, listInvites, createInvite, revokeInvite, previewInvite and acceptInvite, plus the new operation that grants and revokes roles (a contract change, UD-14.d); updateMember and revokeInvite have no rekord-api test and are built test-first (UD-15.d); planners list members, invite as PLANNER or DJ, and list and revoke invites (UD-14.c); only an admin invites as ADMIN, changes a member's status, removes a member or changes roles (UD-14.d); the last-active-admin guard answered with 400 LAST_ADMIN replaces the last-planner guard (UD-14.e); revoking a member's last role is refused with 422 (UD-14.a, UD-12); the membership writes that answer 500 in rekord-api (RISK-39); a change of a member's role set ends every session of that member, and only an admin changes another member's display_name (UD-19.g); removing a member clears the team-slot and vendor-contact links to that member (RISK-34, UD-19.i3, P1-E05-T07). Every item touches the access matrix and is a STOP item. Out of scope: the invite page of the apps (RISK-42, a frontend matter).

Plan item `P1-E02` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner without the ADMIN role When the planner calls createInvite with the role ADMIN Then the request is refused with 403 and no invite exists afterwards (UD-14.d).
- [ ] #2 Given a business whose only active admin is the caller When the caller revokes their own ADMIN role, disables their own membership or removes themselves with deleteMember Then each request answers 400 LAST_ADMIN and the caller keeps the ADMIN role and the active status (UD-14.e).
- [ ] #3 Given a member holding only the DJ role When an admin revokes that role through the new role operation Then the answer is 422 VALIDATION_FAILED and the member still holds DJ (UD-14.a).
- [ ] #4 Given an open invite for the PLANNER role When the invitee calls previewInvite and then acceptInvite with a password Then a member holding the PLANNER role exists and login with the invited address and that password answers 200.
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
