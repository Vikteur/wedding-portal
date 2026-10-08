---
id: TASK-8.6
title: >-
  P1-E02-T06 Invite a planner, DJ or admin and list open invitations
  (createInvite, listInvites)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-8.2
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:98-121'
  - 'rekord-api/src/main/java/app/rekord/account/InviteService.java:33-81'
  - 'rekord-contract/components/identity.yaml:148-202'
  - 'rekord-contract/paths/planner.yaml:860-904'
  - 'docs/rewrite/analysis/10-identity-access.md:302'
  - 'docs/rewrite/analysis/10-identity-access.md:578'
  - 'rekord-contract/components/common.yaml:149-152'
  - 'rekord-api/pom.xml:180-221'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10206
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to invite a new planner or DJ by e-mail and see the invitations still open so that people join the business without a sign-up page.

Builds the operations createInvite and listInvites. Oracle: AccountsResource.createInvite and listInvites (AccountsResource.java:98-121) and InviteService.invite (InviteService.java:33, :46-81): the address is trimmed and lower-cased, blank is 422 "An email address is required.", an address that already has an account in the business is 409 DUPLICATE_USERNAME "That address already has an account here.", the role defaults to DJ, the invite is valid 14 days, only the SHA-256 hash of the token is stored and accept_url is in the creating answer only (BR-ID-32, BR-PL-52). listInvites returns the invites that are neither accepted nor revoked, newest first, expired ones included. Deviations: inviting as ADMIN is for an admin only (UD-14.c, UD-14.d); a second open invite for the same address, expired or not, is refused with 422 instead of failing on ux_dj_invites_open as 500 (RISK-39, PIN-10-0578). Two calls that race past that check clash on ux_dj_invites_open; the persistence adapter maps the clash to the same refusal, answered with 409 (deviation UD-19.i5, P0-E05-T05). New message text (no oracle): the 422 "email already has an open invitation". The contract check comes first: InviteCreate.email is $ref Email, type string with format email (rekord-contract/components/common.yaml:149-152), and openapi-generator 7.25.0 jaxrs-spec writes @NotNull @Email on the getter of the generated InviteCreate (its template JavaJaxRS/spec/beanValidationCore.mustache emits @Email for format email; checked by generating the model from rekord-contract/dist/openapi.yaml with the configOptions of rekord-api/pom.xml:180-221). rekord-api runs the same generator on the same contract, so in both a non-empty value without a well-formed address, "   " included, is refused with 422 before the use case runs, and wedding-portal gives it the item code INVALID_FORMAT (UD-19.d1, UD-19.d2). Hibernate Validator's @Email accepts the empty string, so only "" reaches the blank check of InviteService. docs/rewrite/analysis/08-api-behaviour.md:224 lists format email as not enforced; for 7.25.0 that line is wrong. The vendor_id of the body is stored; its business check is P1-E09-T04 and the contact link on acceptance is P1-E03-T07. The accept address is the setting app.public-base-url followed by /invite/ and the token (AccountsResource.java:48-49, :119-120). The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `createInvite`, `listInvites`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-ID-32, BR-PL-52, PIN-10-0427, PIN-10-0578, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E02-T06` (user-story,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner and the public base address https://portal.example.com When the planner calls createInvite with email New.DJ@example.com and no role Then the answer is 201 with email new.dj@example.com, role DJ, expires_at 14 days after the call and an accept_url of https://portal.example.com/invite/ followed by a 43-character token, and the stored row holds the SHA-256 hash of the token and never the token (BR-ID-32, BR-PL-52).
- [ ] #2 Given that invite When the planner calls listInvites Then the answer is 200 with the invite and accept_url null, an open invite whose expires_at has passed is listed with its expires_at (AccountsResource.java:98-121), and an accepted and a revoked invite of the business are not listed.
- [ ] #3 Given a planner without ADMIN When the planner calls createInvite with role PLANNER, and with role ADMIN Then the first answers 201 and the second answers 403 FORBIDDEN "This is not yours to open." with no invite stored (UD-14.c, deviation UD-14.d).
- [ ] #4 Given an admin When the admin calls createInvite with role ADMIN Then the answer is 201 with role ADMIN.
- [ ] #5 Given a body with email "" When a planner calls createInvite Then the answer is 422 VALIDATION_FAILED "An email address is required." with one errors item {field email, code INVALID_VALUE}, and no invite exists.
- [ ] #6 Given a body with email "   " and a body with email "not-an-address" When a planner calls createInvite with each Then each answer is 422 VALIDATION_FAILED "email must be a well-formed email address" with one errors item {field email, code INVALID_FORMAT, message "must be a well-formed email address"}, and no invite exists (UD-19.d1, UD-19.d2).
- [ ] #7 Given a member of the business with the address dj@example.com When a planner calls createInvite with DJ@example.com Then the answer is 409 DUPLICATE_USERNAME "That address already has an account here." and no invite is stored.
- [ ] #8 Given an open invite for guest.dj@example.com, and an open invite for late.dj@example.com that expired yesterday When a planner calls createInvite for each address again Then both answers are 422 VALIDATION_FAILED "email already has an open invitation" with one errors item {field email, code INVALID_VALUE}, and no new invite is stored (deviation RISK-39; rekord-api answers 500, PIN-10-0578).
- [ ] #9 Given an invite for guest.dj@example.com whose revoked_at the test set directly When a planner calls createInvite for guest.dj@example.com Then the answer is 201 and the business holds one open invite for that address.
- [ ] #10 Given a member holding only DJ When the member calls createInvite or listInvites Then both answers are 403 and no invite is stored.
- [ ] #11 Given no invite for race.dj@example.com and a test latch in the invite repository adapter that holds both calls after the open-invite check When two planners call createInvite for race.dj@example.com at the same time Then one answers 201 and the other answers 409 VALIDATION_FAILED "email already has an open invitation" with the errors item {field email, code INVALID_VALUE}, and the business holds one open invite for that address (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
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
