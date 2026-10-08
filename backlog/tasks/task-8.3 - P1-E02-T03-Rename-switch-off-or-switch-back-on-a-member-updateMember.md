---
id: TASK-8.3
title: 'P1-E02-T03 Rename, switch off or switch back on a member (updateMember)'
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - test-first
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.2
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:59-81'
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:135-172'
  - 'rekord-api/src/main/java/app/rekord/security/SessionService.java:127-135'
  - 'rekord-contract/paths/planner.yaml:797-834'
  - 'docs/rewrite/analysis/10-identity-access.md:299-301'
  - 'docs/rewrite/analysis/10-identity-access.md:580'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10203
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to rename a member and switch a member off or back on so that someone who leaves loses access at once and keeps their history.

Builds the operation updateMember. Oracle: AccountsResource.updateMember (AccountsResource.java:59-81): display_name is set when sent; a status writes the account's status and every membership of the account in this business (BR-ID-29); DISABLED first runs the last-planner guard and revokes every session of the account in every business; vendor_id in the body is ignored; the answer is the whole member list. rekord-api has no test for updateMember, so the ticket is test-first (UD-15.d). Deviations: only an admin changes a status (UD-14.d), answered 403 FORBIDDEN "This is not yours to open." for a planner without ADMIN; the last-planner guard is replaced by the last-admin guard (UD-14.e); status INVITED is refused with 422 instead of reaching the membership check as a 500 (RISK-39, PIN-10-0580). New message texts (no oracle): LAST_ADMIN "The last admin cannot be switched off — there would be nobody left to invite one back." and the 422 "status must be ACTIVE or DISABLED". Deviation UD-19.g: only an admin changes another member's display_name; a planner without ADMIN who sends display_name for another member gets 403 FORBIDDEN "This is not yours to open." (rekord-api answers 200), and a planner still changes their own display_name. An unknown id, a removed member or a member of another business is 404 NO_USER "There is no such account." (AccountsResource.java:135-147).

- Builds: `updateMember`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-ID-29, BR-ID-31, PIN-10-0424, PIN-10-0580
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E02-T03` (user-story,P1,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for updateMember When a characterization test calls updateMember against rekord-api with a new display_name, with status DISABLED, with status ACTIVE, with status INVITED, for the last planner and for an unknown id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given an ACTIVE DJ signed in on two devices When an admin calls updateMember for the DJ with status DISABLED Then the answer is 200 with the member list showing the DJ as DISABLED, the account and every membership of the DJ in the business are DISABLED, and both devices' sessions answer 401 NOT_SIGNED_IN (BR-ID-29).
- [ ] #3 Given that DISABLED DJ When an admin calls updateMember with status ACTIVE Then the answer is 200, the account and its memberships are ACTIVE, the revoked sessions stay revoked and login with the DJ's password answers 200.
- [ ] #4 Given a planner without ADMIN When the planner calls updateMember for a DJ with status DISABLED Then the answer is 403 FORBIDDEN "This is not yours to open." and the DJ stays ACTIVE (deviation UD-14.d; rekord-api answers 200).
- [ ] #5 Given a planner without ADMIN named "Bo" When the planner calls updateMember for a DJ with only display_name "DJ Nova", and then for themselves with only display_name "Bo Planner" Then the first answers 403 FORBIDDEN "This is not yours to open." and the DJ keeps its display_name, and the second answers 200 with the member list showing the planner as "Bo Planner" (deviation UD-19.g; rekord-api answers 200 to the first).
- [ ] #6 Given a business whose only active admin is the caller When the caller calls updateMember for themselves with status DISABLED Then the answer is 400 LAST_ADMIN "The last admin cannot be switched off — there would be nobody left to invite one back." and the caller stays ACTIVE (deviation UD-14.e).
- [ ] #7 Given a business with two active admins and no planner When one admin switches the other off with updateMember Then the answer is 200, because the guard counts admins and a business with zero planners is allowed (UD-14.e).
- [ ] #8 Given an ACTIVE DJ When an admin calls updateMember for the DJ with status INVITED Then the answer is 422 VALIDATION_FAILED "status must be ACTIVE or DISABLED" with one errors item {field status, code INVALID_VALUE}, and nothing changes (deviation RISK-39, UD-19.d1; rekord-api answers 500, PIN-10-0580).
- [ ] #9 Given an unknown member id, a removed member and a member of another business When an admin calls updateMember for each Then each answer is 404 NO_USER "There is no such account." and no row changes (BR-ID-31).
- [ ] #10 Given a body with vendor_id set to a vendor id When an admin calls updateMember Then the answer is 200 and no vendor link of the member changes, because vendor_id is ignored (AccountsResource.java:59-81).
- [ ] #11 Given an admin When the admin calls updateMember on /api/org/members/not-a-uuid Then the answer is 404 NO_WEDDING "There is nothing here." (PIN-10-0424).
- [ ] #12 Given an admin and a DJ with the display_name "Nova" When the admin calls updateMember for the DJ with only display_name "DJ Nova" Then the answer is 200 and the member list shows the DJ as "DJ Nova" (UD-19.g).
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
