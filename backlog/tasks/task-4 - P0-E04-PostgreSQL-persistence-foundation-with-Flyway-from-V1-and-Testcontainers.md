---
id: TASK-4
title: >-
  P0-E04 PostgreSQL persistence foundation with Flyway from V1 and
  Testcontainers
status: To Do
assignee: []
created_date: '2026-10-07 05:45'
labels:
  - epic
  - P0
milestone: m-0
dependencies:
  - TASK-2
references:
  - 'docs/rewrite/STATUS.md:235-236'
  - 'docs/rewrite/STATUS.md:279-286'
  - 'docs/rewrite/architecture-conventions.md:541-628'
  - 'docs/rewrite/analysis/41-domain-model-and-glossary.md:1159-1185'
  - 'rekord-api/src/main/resources/application.properties:10-23'
  - docs/rewrite/analysis/15-data-model-persistence.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
priority: high
ordinal: 400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want every saved record kept in PostgreSQL with a schema that only versioned migrations change so that my data stays intact.

Goal: wedding-portal owns its database. Scope: PostgreSQL (UD-6) with Testcontainers for every test that needs it (UD-7); an empty database with its own Flyway history from V1, no data import, no strangler and no database shared with rekord-api (UD-13.b); Flyway at start with baseline-on-migrate off (BR-DM-01), SQL files with LF endings (BR-DM-04), Hibernate validating and never changing the schema, times in UTC (BR-OPS-23); a database constraint clash never leaks a persistence exception: the persistence adapter translates a clash on a named unique constraint that a domain check guards into the refusal that check raises, inside the port call, and the race answers 409 with that check's code, message and errors list, and any other persistence failure, a failure raised at commit included, answers 500 UNKNOWN (UD-19.i5; architecture-conventions §8.2), pinned in P0-E05-T05; optimistic locking on song lists comes with the song lists in phase 2 (BR-DM-40, moved); the SmallRye health and metrics endpoints; the datasource-less profile for resource tests and WireMock 3 for gateway tests. Which tables each migration creates is settled by the ticket that needs them, with rekord-api/src/main/resources/db/migration as the oracle schema. Out of scope: the SQLite importer and its rules, superseded by UD-13.

Plan item `P0-E04` (epic,P0) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an empty PostgreSQL database started by Testcontainers When the application starts Then Flyway applies the migrations from V1 in order, Hibernate validates the mapping against the schema without changing it, and the JDBC and JVM time zone is UTC.
- [ ] #2 Given a database that holds tables but no Flyway history When the application starts against it Then start-up fails because baseline-on-migrate is off.
- [ ] #3 Given the resource-test profile When a @QuarkusTest starts with it Then Quarkus starts without a datasource, Flyway or Dev Services.
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
