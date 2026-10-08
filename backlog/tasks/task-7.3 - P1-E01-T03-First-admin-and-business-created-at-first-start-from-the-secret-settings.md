---
id: TASK-7.3
title: >-
  P1-E01-T03 First admin and business created at first start from the secret
  settings
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - technical
  - P1
  - stop-auth-access
  - stop-production-config
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.2
references:
  - 'rekord-api/src/main/java/app/rekord/account/Bootstrap.java:40-126'
  - 'docs/rewrite/STATUS.md:317-320'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:68'
  - 'docs/rewrite/analysis/10-identity-access.md:305'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: task
ordinal: 10103
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want the first admin account and its business created at first start from the secret settings so that the user signs in once and creates the planners from there.

UD-14.g. Oracle: Bootstrap.run (Bootstrap.java:61-126) with the settings app.bootstrap.email, app.bootstrap.password, app.bootstrap.display-name (default "The planner") and app.bootstrap.org-name (default "Rekord Match") (Bootstrap.java:49-59). rekord-api creates a PLANNER when the whole database has no planner; wedding-portal creates an account holding only the ADMIN role when the database has no active admin (a membership with the role ADMIN and status ACTIVE whose account is ACTIVE and not deleted), a deliberate deviation (UD-14.g). The settings come from the deployment's secret settings and never from git, which makes this a production-configuration STOP item next to authentication. rekord-api logs the address in clear (Bootstrap.java:125); wedding-portal logs no e-mail address and no name, and names the new account and business by their ids only (deviation UD-19.f, P0-E06-T05). The password rule of at least 12 characters stays (BR-DM-13). The account can change its password with changePassword (P1-E01-T09).

- STOP (human approval in the pull request): auth-access, production-config, crypto-logging
- Covers: UD-14.g, BR-ID-35, BR-DM-13, PIN-15-0068

Plan item `P1-E01-T03` (technical,P1,stop-auth-access,stop-production-config,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an empty database and the settings app.bootstrap.email " Admin@example.com " and app.bootstrap.password of 16 characters When wedding-portal starts Then exactly one business (name "Rekord Match", time zone Europe/Amsterdam, slug rekord-match), one ACTIVE account with the address admin@example.com and the display name "The planner", and one ACTIVE membership with the role ADMIN exist, and Passwords.verify of the configured password against the stored password_hash succeeds (deviation UD-14.g; rekord-api creates a PLANNER).
- [ ] #2 Given a database that already holds an active admin When wedding-portal starts again with any bootstrap settings Then no business, account or membership is created or changed (BR-ID-35).
- [ ] #3 Given an empty database and a missing or blank app.bootstrap.email or app.bootstrap.password When wedding-portal starts Then it starts normally and no business, account or membership exists (BR-DM-13).
- [ ] #4 Given an empty database and an app.bootstrap.password of 11 characters When wedding-portal starts Then it starts normally, no business, account or membership exists, and one ERROR line states that the bootstrap password needs at least 12 characters without containing the password (PIN-15-0068).
- [ ] #5 Given a successful bootstrap When the log lines of the start are captured Then one INFO line names the id of the new account and the id of its business, and no line holds the password, the text admin@example.com, admin@ or @example.com, or the display name "The planner" (deviation UD-19.f; rekord-api logs the address at Bootstrap.java:125).
- [ ] #6 Given the stored account of the bootstrap When its password_hash is read Then it has the form scrypt$16384$8$1$<32 hex salt>$<64 hex hash> and the settings value of the password appears in no column.
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
