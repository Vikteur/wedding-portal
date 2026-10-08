---
id: TASK-4.1
title: >-
  P0-E04-T01 PostgreSQL datasource with its own Flyway history from V1,
  Hibernate validate and UTC
status: Done
assignee:
  - '@archon'
created_date: '2026-10-07 05:45'
updated_date: '2026-10-07 11:30'
labels:
  - user-story
  - P0
  - stop-db-migration
milestone: m-0
dependencies:
  - TASK-1.3
references:
  - 'rekord-api/src/main/resources/application.properties:3-23'
  - 'rekord-api/.gitattributes:9'
  - 'rekord-api/src/test/java/app/rekord/SchemaMigrationTest.java:36-46'
  - 'docs/rewrite/architecture-conventions.md:578-596'
  - 'docs/rewrite/STATUS.md:279-286'
  - 'rekord-api/src/main/resources/application.properties:18'
  - 'rekord-api/src/main/resources/application.properties:8'
  - 'rekord-api/src/main/resources/application.properties:71'
  - 'rekord-api/Dockerfile:65'
documentation:
  - docs/rewrite/architecture-conventions.md
parent_task_id: TASK-4
priority: high
type: feature
ordinal: 401
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want wedding-portal to start on its own empty PostgreSQL database whose schema only Flyway changes so that no stray or imported data reaches my weddings.

UD-6 picks PostgreSQL; UD-13.b gives wedding-portal an empty database and its own Flyway history from V1, with no import, no strangler and no database shared with rekord-api. rekord-api's settings are the oracle (rekord-api/src/main/resources/application.properties:3-23): Flyway at start from classpath:db/migration, baseline-on-migrate false, Hibernate validate and the JDBC time zone UTC (BR-DM-01, BR-OPS-23), the JVM time zone UTC (rekord-api/Dockerfile:65; set in the image by P0-E01-T03) and LF endings for SQL files (BR-DM-04; rekord-api/.gitattributes:9). rekord-api tests only that migrations apply (rekord-api/src/test/java/app/rekord/SchemaMigrationTest.java:36-46); the refusal on a database without Flyway history is untested there. Business tables arrive with the tickets that need them, each a STOP item; the V1 baseline itself is a migration and so a STOP item.

- STOP (human approval in the pull request): db-migration
- Covers: UD-6, UD-13.b, BR-DM-01, BR-DM-02, BR-DM-04, BR-OPS-23

Plan item `P0-E04-T01` (user-story,P0,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given an empty postgres:17-alpine database started by Quarkus Dev Services through Testcontainers with %test reuse=false (rekord-api/src/main/resources/application.properties:8, :71) When the application starts Then Flyway runs at start from classpath:db/migration, held in application/src/main/resources/db/migration, applies V1__baseline.sql, which holds only a comment and creates no table, view, sequence or extension, and flyway_schema_history holds one successful row for version 1.
- [x] #2 Given a PostgreSQL database that holds a table but no flyway_schema_history When the application starts against it Then start-up fails because baseline-on-migrate is false.
- [x] #3 Given a test-only entity mapped to a table that no migration creates When the application starts Then start-up fails because Hibernate runs with schema management validate, and Hibernate sends no DDL.
- [x] #4 Given the running application When a timestamp is written and read back through JDBC Then the JDBC time zone is UTC.
- [x] #5 Given the Gradle test task, which sets -Duser.timezone=UTC in its jvmArgs as the image of P0-E01-T03 sets it in JAVA_OPTS (rekord-api/Dockerfile:65), When a @QuarkusTest reads TimeZone.getDefault().getID() Then it is UTC.
- [x] #6 Given the .gitattributes of wedding-portal When a *.sql file is checked out on Windows and on Linux Then its line endings are LF on both, so the Flyway checksum is the same.
- [x] #7 Given the datasource settings of every profile When they are read Then db-kind is postgresql, and no setting names a rekord-api database, a SQLite file or an import job (UD-13.b).
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [x] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [x] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [x] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [x] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [x] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Finalization evidence (PR https://github.com/Vikteur/wedding-portal/pull/5, merged as 274bcfc91cf2cfbe6b84dd3fd6ceb34329253ba2; head e15ab9f01f4ce048a50be261af0437cfbd1b5a5a; CI run 37609998380 'CI' on that SHA: success, jobs build and image green; build ran :application:test, :application:integrationTest and :application:startupTest, BUILD SUCCESSFUL; image job printed 'Check: flyway ran in the started image' and 'OK: wedding-portal:ci'; the only skipped step is 'Upload test reports', which runs only on failure; no @Disabled/@Ignore/assume* in application on main).
- AC #1: SchemaMigrationIT (one successful V1 row, no table/view/sequence/extension beyond flyway_schema_history/plpgsql, PostgreSQL 17 via Dev Services), BaselineMigrationTest (only V1, comment-only), DatasourceSettingsTest (%test reuse=false, postgres:17-alpine, migrate-at-start, classpath:db/migration).
- AC #2: FlywayRefusesADatabaseWithoutHistoryIT (start-up fails with 'no schema history table'; database left untouched).
- AC #3: HibernateValidateRefusesAnUnmigratedEntityIT (SchemaManagementException 'missing table [stray_entity]'; stray_entity never created), DatasourceSettingsTest (validate, no generation/load script).
- AC #4: JdbcTimeZoneIT (session timezone UTC; +02:00 timestamp reads back as 10:00Z and '+00').
- AC #5: TimeZoneIT (@QuarkusTest zone UTC and -Duser.timezone=UTC in JVM args), TestJvmTimeZoneTest.
- AC #6: GitAttributesTest ('*.sql text eol=lf', git check-attr eol: lf, no CR in any SQL file), BaselineMigrationTest (no CR), run by CI on Linux and locally on Windows.
- AC #7: DatasourceSettingsTest (every db-kind postgresql; no rekord-api/sqlite/.db/import/rekord db-name; %prod only ${DB_URL}/${DB_USER}/${DB_PASSWORD}).
- DoD #1: the tests above each fail when their criterion breaks. DoD #2: CI run 37609998380 green on e15ab9f, nothing skipped or disabled.
- DoD #3 N/A: wedding-portal has no ArchUnit rules or frozen baseline yet (no ArchUnit reference on main); this ticket adds none.
- DoD #4 N/A: no contract change; the ticket touches only persistence settings and tests.
- DoD #6 N/A: the ticket builds no operation and no refusal answered over HTTP.
- DoD #7 N/A: the ticket builds no operation; no UD deviation implemented.
- DoD #8: %prod reads DB_URL/DB_USER/DB_PASSWORD from the environment with no default; image-check.sh generates the password per run (openssl rand) and never echoes it; DockerfileTest refuses a literal POSTGRES_PASSWORD; test containers use Testcontainers-generated credentials.
- DoD #9: the only schema change is the new V1__baseline.sql, exercised by SchemaMigrationIT and both start-up ITs on Testcontainers PostgreSQL.
- DoD #10: docs/memory.md section '2026-10-07 — TASK-4.1 datasource and Flyway' records the decisions, including the startupTest task.
NOT CHECKED, status left In Progress:
- DoD #5: the PR adds the db-migration STOP item V1__baseline.sql and the %prod datasource settings, but PR #5 has no review, no approval and no comment recording a human approval (timeline: ready_for_review then merged, both by the author Vikteur). The merge itself is not an approval recorded before the step. Needs a human to record the STOP approval (for example a comment on PR #5) before this can be checked.
- DoD #11: PR #5 is merged into main (274bcfc) but has no review recorded (reviewDecision empty, reviews []). Needs the review to be recorded or the rule waived by a human.
Follow-ups named, not created: TASK-4.3 (datasource-less test profile, so @QuarkusTest without Docker); an ArchUnit baseline ticket if none is planned.

- DoD #5 and #11 resolved: the user commented "Approved" on PR #5 — https://github.com/Vikteur/wedding-portal/pull/5#issuecomment-6036982123 (Vikteur, 2026-10-07T11:30:04Z), after the head commit e15ab9f. The PR body names the STOP items it approves: the Flyway migration V1__baseline.sql (comment-only) and the %prod datasource settings from DB_URL/DB_USER/DB_PASSWORD. The comment came after the merge; the same STOP items were approved before any code was written at the Archon stop-gate of run 0e287c77. PR merged into main as 274bcfc by Vikteur.
- Push CI on the merge commit 274bcfc (run 37613248824) is green.
- From now on build-feature keeps the pull request a draft until such an "Approved" comment exists (UD-21.d, umbrella a65da37, wedding-portal 9a07940).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
wedding-portal now starts on its own PostgreSQL database whose schema only Flyway changes: an empty comment-only V1__baseline.sql, Flyway at start from classpath:db/migration with baseline-on-migrate=false, Hibernate schema validate, JDBC and test JVMs in UTC, *.sql pinned to LF, %prod connection only from DB_URL/DB_USER/DB_PASSWORD, and the CI image check started against a throwaway postgres:17-alpine.

Evidence: PR https://github.com/Vikteur/wedding-portal/pull/5 merged as 274bcfc; CI run 37609998380 green on head e15ab9f; push run 37613248824 green on 274bcfc; tests SchemaMigrationIT, FlywayRefusesADatabaseWithoutHistoryIT, HibernateValidateRefusesAnUnmigratedEntityIT, JdbcTimeZoneIT, TimeZoneIT, TestJvmTimeZoneTest, GitAttributesTest, BaselineMigrationTest, DatasourceSettingsTest; review and STOP approval recorded on PR #5 (issuecomment-6036982123).
<!-- SECTION:FINAL_SUMMARY:END -->
