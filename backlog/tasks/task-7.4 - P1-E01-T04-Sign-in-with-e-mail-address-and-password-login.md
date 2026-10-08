---
id: TASK-7.4
title: P1-E01-T04 Sign in with e-mail address and password (login)
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - user-story
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.2
  - TASK-7.1
  - TASK-7.3
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:60-109'
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:257-260'
  - 'rekord-api/src/main/java/app/rekord/security/Passwords.java:25-85'
  - 'rekord-contract/paths/auth.yaml:1-48'
  - 'docs/rewrite/analysis/10-identity-access.md:271-277'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: feature
ordinal: 10104
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a member, I want to sign in with my e-mail address and password so that I reach the app my roles open for me.

Builds the operation login. Oracle: AuthResource.login (AuthResource.java:60-109) with Passwords (Passwords.java:25-86); rekord-api tests the sign-in path, so this ticket is not test-first. Kept as in the oracle: the trimmed, lower-cased address; identical answers for an unknown address and a wrong password, with one scrypt check against a dummy hash for the unknown address; the status check after the password; the membership check; expected_role as advisory only; scrypt N=16384, r=8, p=1. ADMIN follows UD-14.b and UD-14.f: an account holding ADMIN satisfies expected_role PLANNER, because getMe reports such an account as PLANNER. Cookies, the session contents and session limits are P1-E01-T06; the throttle is P1-E01-T05. An account never holds ACTIVE memberships in two businesses: an account is created only by acceptInvite, which refuses an address that already has a live account (P1-E02-T08), and by the bootstrap (P1-E01-T03), each with a membership in one business, and no operation adds a membership in another business to an existing account (InviteService.java:112-134, Bootstrap.java:93-122); so the business login puts in the session is the account's only business (BR-ID-05, P1-E01-T06). The audit_log rows this operation writes are added by P1-E09-T10 (UD-19.k).

- Builds: `login`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-ID-01, BR-ID-02, BR-ID-03, BR-ID-04, BR-ID-06, BR-ID-07, PIN-10-0273, PIN-10-0414, UX-05
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E01-T04` (user-story,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an ACTIVE account planner@example.com holding PLANNER in one business with the password "Valid-pass-2027" When a visitor calls login with the address Planner@example.com and that password Then the answer is 200 with a MeResponse holding the account id, email, display_name, role PLANNER, roles [PLANNER] and organization_name, and last_login_at on the account set to the time of the call (BR-ID-01).
- [ ] #2 Given that account and an address no account has When a visitor calls login with the account's address and a wrong password, and with the unknown address Then both answers are 401 BAD_CREDENTIALS "That email and password do not match an account.", and the unknown address still runs one scrypt check against a dummy hash (BR-ID-02).
- [ ] #3 Given a soft-deleted account with the address dj@example.com When a visitor calls login with that address and its former password Then the answer is 401 BAD_CREDENTIALS (BR-ID-01).
- [ ] #4 Given a DISABLED account When a visitor calls login with its right password, and with a wrong one Then the first answers 403 ACCOUNT_DISABLED "This account has been switched off. Ask your planner to turn it back on." and the second answers 401 BAD_CREDENTIALS, because the status is checked only after the password (BR-ID-03).
- [ ] #5 Given an ACTIVE account without any ACTIVE membership When a visitor calls login with its right password Then the answer is 403 FORBIDDEN "This account is not part of any organisation yet." and no sessions row exists (BR-ID-04).
- [ ] #6 Given an account holding only DJ, and an account holding only ADMIN When a visitor calls login for the DJ account with expected_role PLANNER, for the ADMIN account with expected_role PLANNER, and for the ADMIN account with expected_role DJ Then the first answers 403 FORBIDDEN "That account does not sign in here. Try the other sign-in page.", the second and third answer 200, and expected_role never adds a role to the session (BR-ID-06, deviation UD-14.b, UD-14.f; rekord-api answers 403 for an account without the expected role).
- [ ] #7 Given a login body without email, and one without password When a visitor calls login Then the first answers 422 VALIDATION_FAILED with the errors item {field email, code REQUIRED} and the second with {field password, code REQUIRED}, and a 1-character password for an existing address answers 401 BAD_CREDENTIALS because the login password has no length rule (PIN-10-0414, BR-ID-08, UD-19.d1).
- [ ] #8 Given an account whose password_hash was stored as scrypt$16384$8$1$<salt>$<hash>, and an account whose stored hash is malformed When each signs in with its password Then the first answers 200 and the second answers 401 BAD_CREDENTIALS, not 500, and a newly set password is stored with N=16384, r=8, p=1, a 32-byte key and a 16-byte random salt (BR-ID-07).
- [ ] #9 Given the admin created by the bootstrap of P1-E01-T03 When the admin calls login with the configured address, the configured password and expected_role PLANNER Then the answer is 200 with role PLANNER and roles [ADMIN] (deviation UD-14.g).
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
