---
id: TASK-8.2
title: P1-E02-T02 List the members of the business with all their roles (listMembers)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-7.8
  - TASK-8.1
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:33-57'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:174-217'
  - 'rekord-contract/components/identity.yaml:57-100'
  - 'rekord-contract/paths/planner.yaml:782-795'
  - 'docs/rewrite/STATUS.md:304-310'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10202
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to see everyone with an account in my business and every role they hold so that I know who works with me.

Builds the operation listMembers. Oracle: AccountsResource.listMembers and users() (AccountsResource.java:55-57, :174-217): every member of the business whose account is not deleted, disabled members included, sorted by display name, one entry per account with role folded to the widest role. Deviations: the member and invitation operations admit a caller holding PLANNER or ADMIN (UD-14.b, UD-14.c; rekord-api admits PLANNER only, AccountsResource.java:36); role follows UD-14.f (PLANNER when the member holds ADMIN or PLANNER, DJ otherwise) and the new roles list holds every role of the member in this business sorted by name (contract change of P1-E01-T01). vendor_id stays null until P1-E03-T07 links accounts to vendor contacts.

- Builds: `listMembers`
- STOP (human approval in the pull request): auth-access
- Covers: PIN-10-0423
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E02-T02` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a business with an admin "Ada" holding ADMIN, a planner "Bo" holding PLANNER and DJ, a DISABLED DJ "Cas", and a removed member "Dee" When a planner calls listMembers Then the answer is 200 with exactly Ada, Bo and Cas in that order, Ada with role PLANNER and roles [ADMIN], Bo with role PLANNER and roles [DJ, PLANNER], Cas with role DJ, roles [DJ] and status DISABLED (deviation UD-14.f).
- [ ] #2 Given that business When the admin "Ada" calls listMembers Then the answer is 200 with the same list (deviation UD-14.b; rekord-api admits PLANNER only).
- [ ] #3 Given a member holding only DJ When the member calls listMembers Then the answer is 403 and no member data is returned (PIN-10-0423).
- [ ] #4 Given a member of another business When a planner calls listMembers Then that member is not in the answer.
- [ ] #5 Given a member When listMembers returns the member Then the entry holds id, email, display_name, role, roles, status, created_at and last_login_at, with last_login_at null for a member who never signed in.
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
