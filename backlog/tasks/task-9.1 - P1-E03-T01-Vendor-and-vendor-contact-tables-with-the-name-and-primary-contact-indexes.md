---
id: TASK-9.1
title: >-
  P1-E03-T01 Vendor and vendor-contact tables with the name and primary-contact
  indexes
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - technical
  - P1
  - stop-db-migration
milestone: m-1
dependencies:
  - TASK-8.1
references:
  - rekord-api/src/main/resources/db/migration/V3__vendors.sql
  - 'rekord-api/src/main/resources/db/migration/V1__identity.sql:70-96'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:75-76'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:517'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: task
ordinal: 10301
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the vendor tables in a Flyway migration of wedding-portal so that the directory, its contacts and the invitation link to a vendor have a schema.

Oracle: V3__vendors.sql, kept with its columns: vendors with the category check (CATERING, PHOTO, LOCATION, DJ, FLORIST, MC, OTHER), archived_at and deleted_at; the unique index over business, category and lower(name) among rows with deleted_at null, so archived vendors still count (BR-DM-14); the pg_trgm extension and the trigram index over name and blurb; vendor_contacts with user_id referencing users with ON DELETE SET NULL and at most one primary contact per vendor (BR-DM-15); and the foreign key from dj_invites.vendor_id to vendors with ON DELETE SET NULL that V3 adds to the invitation table of P1-E02-T01. A new Flyway migration of wedding-portal.

- STOP (human approval in the pull request): db-migration

Plan item `P1-E03-T01` (technical,P1,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the migrations of P1-E01 and P1-E02 When wedding-portal starts on an empty database Then Flyway applies the vendor migration, the tables vendors and vendor_contacts exist with the columns of V3__vendors.sql, the pg_trgm extension is installed and Hibernate validation passes.
- [ ] #2 Given a CATERING vendor "Smaakmakers" When the repository test inserts a CATERING vendor "SMAAKMAKERS" of the same business, then a PHOTO vendor "Smaakmakers" Then the first insert fails on the name index and the second succeeds (BR-DM-14).
- [ ] #3 Given an archived CATERING vendor "Smaakmakers" When the repository test inserts a CATERING vendor "Smaakmakers" of the same business Then the insert fails on the name index, because archived vendors are not deleted.
- [ ] #4 Given a vendor with one primary contact When the repository test inserts a second primary contact for it Then the insert fails on the primary-contact index (BR-DM-15).
- [ ] #5 Given a vendor contact linked to an account When the repository test deletes the account row Then the contact remains with user_id null.
- [ ] #6 Given an invite naming a vendor When the repository test inserts an invite with a vendor_id that matches no vendor Then the insert fails on the invitation's vendor foreign key.
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
