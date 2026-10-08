---
id: TASK-8.1
title: >-
  P1-E02-T01 Invitation table with one open invitation per address and the ADMIN
  role
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - technical
  - P1
  - stop-db-migration
  - stop-crypto-logging
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-7.2
references:
  - 'rekord-api/src/main/resources/db/migration/V1__identity.sql:70-96'
  - rekord-api/src/main/resources/db/migration/V3__vendors.sql
  - 'docs/rewrite/analysis/15-data-model-persistence.md:66'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:515'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-8
priority: high
type: task
ordinal: 10201
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the invitation table in a Flyway migration of wedding-portal so that invites, previews and acceptances have a schema that keeps one open invitation per address.

Oracle: the table dj_invites of V1__identity.sql:70-96, kept with its columns, its unique index over the token hash and its partial unique index ux_dj_invites_open over the business and lower(email) among invites that are neither accepted nor revoked (BR-DM-11). Deviation UD-14.a: the role check holds ADMIN, PLANNER and DJ. The column vendor_id has no foreign key yet; the key to vendors comes with the vendor tables in P1-E03-T01, as in the oracle (V1__identity.sql:76-78, V3__vendors.sql:69-71). The invitation token is stored only as its SHA-256 hash. A new Flyway migration of wedding-portal after the identity migration of P1-E01-T02.

- STOP (human approval in the pull request): db-migration, crypto-logging, auth-access
- Covers: BR-DM-11, PIN-15-0066

Plan item `P1-E02-T01` (technical,P1,stop-db-migration,stop-crypto-logging,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the identity migration of P1-E01-T02 When wedding-portal starts on an empty database Then Flyway applies the invitation migration and the table dj_invites exists with the columns of V1__identity.sql:70-88 and the role check ADMIN, PLANNER, DJ, and Hibernate validation passes.
- [ ] #2 Given an open invite of a business for planner@example.com When the repository test inserts a second open invite of the same business for PLANNER@example.com Then the insert fails on ux_dj_invites_open, and after the first invite gets a revoked_at the second insert succeeds (BR-DM-11).
- [ ] #3 Given an open invite of one business for dj@example.com When the repository test inserts an open invite of another business for the same address Then the insert succeeds, because the index is per business.
- [ ] #4 Given two invites with the same token_hash When the repository test inserts both Then the second fails on the unique token index.
- [ ] #5 Given an invite row with the role OWNER When the repository test inserts it Then the insert fails on the role check, and the roles ADMIN, PLANNER and DJ are accepted (UD-14.a).
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
