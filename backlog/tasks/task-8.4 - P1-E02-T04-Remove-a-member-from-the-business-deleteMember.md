---
id: TASK-8.4
title: P1-E02-T04 Remove a member from the business (deleteMember)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.3
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:83-96'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:149-172'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:127-135'
  - 'rekord-contract/paths/planner.yaml:836-858'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:102'
  - 'docs/rewrite/analysis/10-identity-access.md:300'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10204
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to remove a member from the business so that someone who left can no longer sign in while the weddings they worked keep their history.

Builds the operation deleteMember. Oracle: AccountsResource.deleteMember (AccountsResource.java:83-96): the guard runs, every session of the account in every business is revoked, the memberships of the account in this business are deleted and the account gets a deleted_at, so its address becomes free for a new account (BR-ID-30, BR-DM-36). Deviations: only an admin removes a member (UD-14.d), answered 403 FORBIDDEN "This is not yours to open." for a planner without ADMIN; the last-admin guard replaces the last-planner guard (UD-14.e). New message text (no oracle): LAST_ADMIN "The last admin cannot be removed — there would be nobody left to invite one back.". Clearing the team-slot and vendor-contact links to a removed account is P1-E05-T07 (UD-19.i3). The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `deleteMember`
- STOP (human approval in the pull request): auth-access
- Covers: BR-ID-30
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E02-T04` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ signed in on one device When an admin calls deleteMember for the DJ Then the answer is 200 with a member list without the DJ, the DJ's memberships in the business are gone, the account carries a deleted_at, the session answers 401 NOT_SIGNED_IN, and login with the DJ's address answers 401 BAD_CREDENTIALS (BR-ID-30).
- [ ] #2 Given that removed DJ, whose account and membership were inserted by the test When the test inserts a new ACTIVE account with the same address Then the insert succeeds and the address belongs to a new account with a new id (BR-DM-08).
- [ ] #3 Given a DJ of the business and, inserted directly by the test, a membership of the same account in a second business with a session there (a state no phase-1 operation can create) When an admin of the first business removes the DJ Then the membership in the second business stays, the session in the second business answers 401 NOT_SIGNED_IN, and the account carries a deleted_at (BR-DM-36; kept as in rekord-api, PIN-15-0102).
- [ ] #4 Given a planner without ADMIN When the planner calls deleteMember for a DJ Then the answer is 403 FORBIDDEN "This is not yours to open." and the DJ stays a member (deviation UD-14.d; rekord-api answers 200).
- [ ] #5 Given a business whose only active admin is the caller When the caller calls deleteMember for themselves Then the answer is 400 LAST_ADMIN "The last admin cannot be removed — there would be nobody left to invite one back." and the caller stays a member (deviation UD-14.e).
- [ ] #6 Given an unknown member id and a member of another business When an admin calls deleteMember for each Then each answer is 404 NO_USER "There is no such account." and no row changes.
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
