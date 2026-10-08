---
id: TASK-7.10
title: P1-E01-T10 Break-glass server command that sets a new password for an account
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - technical
  - P1
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.6
references:
  - 'rekord-api/src/main/java/app/rekord/security/AuthResource.java:136-155'
  - 'rekord-api/src/main/java/app/rekord/account/Bootstrap.java:22-37'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:56'
  - 'docs/rewrite/STATUS.md:341-342'
  - 'docs/rewrite/business-analysis.md:1459'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: task
ordinal: 10110
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want a server command that sets a new password for any account so that a member who lost the password gets back in without an in-app reset.

UD-16.UX-12, RISK-44: rekord-api has no recovery of any kind (S20 UX-12) and wedding-portal adds no in-app reset; the operator runs a command shipped in the wedding-portal image against the configured database. The command is `java -jar wedding-portal.jar reset-password <e-mail address>`; it takes the account's e-mail address as its argument and reads the new password from standard input, so the password never appears in the process list or the shell history. It stores the new scrypt hash with password_changed_at, revokes every session of the account (as changePassword does, AuthResource.java:136-155) and leaves the account status and roles unchanged. The new password needs at least 8 characters, the minLength of new_password in changePassword (rekord-contract/components/identity.yaml:246, BR-ID-08). Its log lines name the account by its id only and hold neither the password nor the e-mail address (UD-19.f). No HTTP operation is added. This ticket documents the command in the README of the module that ships it.

- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: UD-16.UX-12, UX-12, RISK-44

Plan item `P1-E01-T10` (technical,P1,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an account admin@example.com signed in on one device When the operator runs the break-glass command with that address and types a new password of 8 or more characters Then the command exits with 0, login with the new password answers 200, login with the old one answers 401 BAD_CREDENTIALS, and the device's session answers 401 NOT_SIGNED_IN (UD-16.UX-12).
- [ ] #2 Given no account with the address nobody@example.com When the operator runs the command with that address Then the command exits with a non-zero code, prints that no account has that address, and no row changes.
- [ ] #3 Given an account When the operator runs the command with a new password of 7 characters Then the command exits with a non-zero code, prints that the password needs at least 8 characters, and the stored hash is unchanged.
- [ ] #4 Given a DISABLED account When the operator sets a new password with the command Then the status stays DISABLED and login with the new password answers 403 ACCOUNT_DISABLED.
- [ ] #5 Given a run of the command for admin@example.com When its output and the log lines are captured Then no line holds the password, no log line holds admin@example.com, admin@ or @example.com, and the log lines name the account by its id (UD-19.f).
- [ ] #6 Given the generated interfaces of the pinned contract When the route-guard test of P0-E06-T01 runs Then the public allow-list is unchanged, because no operation resets a password over HTTP (RISK-44).
- [ ] #7 Given the README of the module that ships the command When it is read Then it names the command java -jar wedding-portal.jar reset-password, its one argument, the account's e-mail address, that the new password is read from standard input, the minimum of 8 characters, and the exit code 0 on success and a non-zero exit code when no account has the address or the password is too short.
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
