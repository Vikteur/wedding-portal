---
id: TASK-8.8
title: >-
  P1-E02-T08 Preview and accept an invitation, creating the account
  (previewInvite, acceptInvite)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-8.6
  - TASK-8.3
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:157-184'
  - 'rekord-api/src/main/java/app/rekord/account/InviteService.java:83-162'
  - 'rekord-contract/components/identity.yaml:204-233'
  - 'rekord-contract/paths/auth.yaml:102-163'
  - 'docs/rewrite/analysis/10-identity-access.md:304'
  - 'docs/rewrite/analysis/10-identity-access.md:579'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: feature
ordinal: 10208
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an invitee, I want to see what I am invited to and accept it with my name and a password so that I arrive signed in without a separate sign-up.

Builds the operations previewInvite and acceptInvite, both public (allow-list of P0-E06-T01). Oracle: AuthResource.previewInvite and acceptInvite (AuthResource.java:159-184) and InviteService.resolve and accept (InviteService.java:84-154): an unknown, accepted or revoked token is 404 INVITE_INVALID "This invitation link is not valid.", an expired one is 410 INVITE_EXPIRED "This invitation has expired. Ask for a new one."; accepting creates an ACTIVE account and a membership with the invited role, stamps the invite as accepted, opens a session and answers 201 with the rm_session cookie (BR-ID-34). The password needs 8 characters through the contract's minLength (BR-ID-08). Deviation RISK-39: an address that already has a live account anywhere is refused with 422 instead of failing on the unique address index as 500 (PIN-10-0579). New message text (no oracle): the 422 "email already has an account". Its errors item carries field null (UD-19.d1): the InviteAccept body holds only display_name and password, and the address the rule checks comes from the invite, not from the request (rekord-contract/components/identity.yaml:222-233). Two acceptances that race past that check clash on ux_users_email; the persistence adapter maps the clash to the same refusal, answered with 409 (deviation UD-19.i5, P0-E05-T05). The contact link for an invite that names a vendor is P1-E03-T07. The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `previewInvite`, `acceptInvite`
- STOP (human approval in the pull request): auth-access
- Covers: BR-ID-34, PIN-10-0419, PIN-10-0579, RISK-39, BR-ID-08, BR-ID-33, UD-19.i5
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E02-T08` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an open invite of the business "Rekord Match" for new.dj@example.com as DJ, sent by the planner "Bo" When a visitor calls previewInvite with its token Then the answer is 200 with organization_name "Rekord Match", email new.dj@example.com, role DJ and invited_by "Bo", and nothing else about the business.
- [ ] #2 Given that invite When the visitor calls acceptInvite with display_name "Nova" and password "Valid-pass-2027" Then the answer is 201 with a MeResponse of role DJ and roles [DJ] and a Set-Cookie rm_session, an ACTIVE account and an ACTIVE DJ membership exist, the invite carries accepted_at and the new account as accepted_user_id, and login with the address and password answers 200 (BR-ID-34).
- [ ] #3 Given an open invite as ADMIN When the invitee accepts it Then the new member holds only ADMIN and getMe answers role PLANNER and roles [ADMIN] (UD-14.a, UD-14.f).
- [ ] #4 Given an accepted invite, a revoked invite and a token that matches no invite When a visitor calls previewInvite and acceptInvite with each Then every answer is 404 INVITE_INVALID "This invitation link is not valid." and no account is created.
- [ ] #5 Given an invite whose expires_at has passed When a visitor calls previewInvite and acceptInvite with its token Then both answer 410 INVITE_EXPIRED "This invitation has expired. Ask for a new one.".
- [ ] #6 Given an open invite When a visitor calls acceptInvite with a 7-character password, and with an empty display_name Then both answer 422 VALIDATION_FAILED, the first with one errors item {field password, code TOO_SHORT} and the second with one errors item {field display_name, code TOO_SHORT}, no account exists and the invite stays open (BR-ID-08, UD-19.d1).
- [ ] #7 Given an open invite for an address that already has a live account in another business When the invitee calls acceptInvite Then the answer is 422 VALIDATION_FAILED "email already has an account" with one errors item {field null, code INVALID_VALUE}, no account or membership is created and the invite stays open (deviation RISK-39, UD-19.d1; rekord-api answers 500, PIN-10-0579).
- [ ] #8 Given open invites for new.dj@example.com in two businesses, inserted by the test, and a test latch in the account repository adapter that holds both calls after the live-account check When both invitees call acceptInvite at the same time Then one answers 201 and the other answers 409 VALIDATION_FAILED "email already has an account" with the errors item {field null, code INVALID_VALUE}, one account with that address exists and the second invite stays open (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
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
