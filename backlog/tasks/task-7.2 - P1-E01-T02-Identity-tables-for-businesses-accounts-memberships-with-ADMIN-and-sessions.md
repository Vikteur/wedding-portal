---
id: TASK-7.2
title: >-
  P1-E01-T02 Identity tables for businesses, accounts, memberships with ADMIN
  and sessions
status: To Do
assignee: []
created_date: '2026-10-07 07:10'
labels:
  - technical
  - P1
  - stop-db-migration
  - stop-auth-access
  - stop-crypto-logging
milestone: m-1
dependencies: []
references:
  - rekord-api/src/main/resources/db/migration/V1__identity.sql
  - rekord-api/src/main/resources/db/migration/V2__sessions.sql
  - rekord-api/src/main/resources/db/migration/V9__membership_roles.sql
  - 'docs/rewrite/analysis/15-data-model-persistence.md:53-67'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:225-236'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-7
priority: high
type: task
ordinal: 10102
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the identity tables in a Flyway migration of wedding-portal so that accounts, roles and sessions have a schema the later operations build on.

The first business tables of wedding-portal's own Flyway history (UD-13: an empty database, no import). Oracle: V1__identity.sql (organizations, users, memberships), V9__membership_roles.sql (the unique key over org, user and role) and V2__sessions.sql (sessions). Deviations: the memberships role check holds ADMIN, PLANNER and DJ (UD-14.a); the users columns failed_login_count and locked_until are left out because nothing reads or writes them (S20 UX-13) and the sign-in throttle of P1-E01-T05 uses its own table. The sessions table keeps the portal columns portal_id and wedding_id of the oracle; their foreign keys to the portal and wedding tables come with those tables in P1-E04-T01. The session token is stored only as its SHA-256 hash (BR-ID-09). Instants are timestamptz and the JDBC time zone is UTC (BR-DM-03, P0-E04-T01). dj_invites comes with P1-E02-T01. Repository tests run on Testcontainers PostgreSQL (P0-E04-T02).

- STOP (human approval in the pull request): db-migration, auth-access, crypto-logging
- Covers: BR-DM-03, BR-DM-08, BR-DM-09, BR-DM-10, BR-DM-12, PIN-15-0053, PIN-15-0063, PIN-15-0067

Plan item `P1-E01-T02` (technical,P1,stop-db-migration,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an empty PostgreSQL database When wedding-portal starts Then Flyway applies the identity migration and the tables organizations, users, memberships and sessions exist with the columns of V1__identity.sql and V2__sessions.sql, except users.failed_login_count and users.locked_until, and Hibernate validation passes.
- [ ] #2 Given two users whose e-mail addresses differ only in case, planner@example.com and Planner@example.com, When the repository test inserts both Then the second insert fails on the unique index over lower(email) among rows with deleted_at null, and after the first row gets a deleted_at the second insert succeeds (BR-DM-08).
- [ ] #3 Given two live businesses whose slugs differ only in case When the repository test inserts both Then the second insert fails on the unique index over lower(slug) among rows with deleted_at null (BR-DM-09).
- [ ] #4 Given a member of a business When the repository test inserts memberships for that member with the roles ADMIN, DJ and PLANNER, then one more with PLANNER, then one with the role OWNER Then the first three succeed, the fourth fails on the unique key over org, user and role, and the fifth fails on the role check (BR-DM-10, UD-14.a).
- [ ] #5 Given the sessions table When the repository test inserts a USER session with a portal_id, a PORTAL session without a wedding_id, and two sessions with the same token_hash Then all three inserts fail, on the subject check for the first two and on the unique token index for the third (BR-DM-12).
- [ ] #6 Given the JVM default zone set to Pacific/Auckland When the repository test stores a session whose created_at is 2027-06-12T10:00:00Z and reads it back after clearing the persistence context Then the instant read back is 2027-06-12T10:00:00Z (BR-DM-03).
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
