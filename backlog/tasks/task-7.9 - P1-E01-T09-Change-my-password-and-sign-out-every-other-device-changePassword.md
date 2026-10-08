---
id: TASK-7.9
title: P1-E01-T09 Change my password and sign out every other device (changePassword)
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - user-story
  - P1
  - test-first
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.6
  - TASK-7.3
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:136-155'
  - 'rekord-contract/components/identity.yaml:235-247'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:89-103'
  - 'docs/rewrite/analysis/10-identity-access.md:278'
  - 'docs/rewrite/analysis/10-identity-access.md:416'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: feature
ordinal: 10109
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want to change my password with my current one so that I take back my account when I think someone else knows the old one.

Builds the operation changePassword. Oracle: AuthResource.changePassword (AuthResource.java:136-155): the current password is checked (401 BAD_CREDENTIALS), the new hash is stored with password_changed_at, every session of the account is revoked and a new rm_session is issued. The minimum of 8 characters comes from the contract's minLength through bean validation (BR-ID-08). rekord-api has no test for changePassword, so the ticket is test-first (UD-15.d). The bootstrapped admin uses this operation to replace the initial password (UD-14.g).

- Builds: `changePassword`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-ID-15, BR-ID-08, PIN-10-0278, PIN-10-0416, PIN-10-0471, PIN-10-0630
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E01-T09` (user-story,P1,test-first,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for changePassword When a characterization test calls changePassword against rekord-api with a right and a wrong current password, a 7-character new password, no session and a portal session Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a member signed in on two devices with the password "Valid-pass-2027" When the member calls changePassword from the first device with that current password and the new password "Other-pass-2028" Then the answer is 204 with a new Set-Cookie rm_session, both old cookies answer 401 NOT_SIGNED_IN, the new cookie answers 200 on getMe, password_changed_at is set, and login with the old password answers 401 BAD_CREDENTIALS while the new one answers 200 (BR-ID-15).
- [ ] #3 Given a signed-in member When the member calls changePassword with a wrong current password Then the answer is 401 BAD_CREDENTIALS "That email and password do not match an account.", the stored hash is unchanged and no session is revoked.
- [ ] #4 Given a signed-in member When the member calls changePassword with a 7-character new_password, and with a body without current_password Then the first answers 422 VALIDATION_FAILED with the errors item {field new_password, code TOO_SHORT}, the second with {field current_password, code REQUIRED}, and the stored hash is unchanged (BR-ID-08, PIN-10-0278, UD-19.d1).
- [ ] #5 Given a request without a session When it calls changePassword Then the answer is 401 NOT_SIGNED_IN.
- [ ] #6 Given a request carrying only a live PORTAL session When it calls changePassword Then the answer is 403 FORBIDDEN "This is not yours to open." and no password changes (PIN-10-0416).
- [ ] #7 Given the admin created by the bootstrap of P1-E01-T03 When the admin calls changePassword with the initial password and a new one of 8 or more characters Then the answer is 204 and only the new password signs in (UD-14.g).
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
