---
id: TASK-15.9
title: >-
  P1-E09-T09 Purge expired sessions, closed invitations and soft-deleted rows
  with a daily cleanup job
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
  - stop-delete-history
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-7.5
  - TASK-8.7
  - TASK-10.5
  - TASK-12.4
  - TASK-7.6
  - TASK-8.4
  - TASK-8.8
  - TASK-10.2
  - TASK-12.3
  - TASK-13.1
  - TASK-14.1
references:
  - 'docs/rewrite/analysis/15-data-model-persistence.md:410-412'
  - 'rekord-api/src/main/resources/db/migration/V2__sessions.sql:81-95'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:105-129'
  - 'docs/rewrite/architecture-conventions.md:832-840'
  - 'rekord-api/src/main/resources/db/migration/V1__identity.sql:84-86'
  - 'docs/rewrite/STATUS.md:434-437'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10909
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an operator of wedding-portal, I want expired sessions, old sign-in attempts, closed invitations and soft-deleted rows removed by a daily job so that the database keeps no row longer than the retention periods the user set.

Deviation UD-19.j (RISK-35, 15 R-15 at 15-data-model-persistence.md:412): rekord-api keeps every row forever and schedules no job although quarkus-scheduler is a dependency. wedding-portal runs one cleanup job, a Quarkus @Scheduled method with the cron "0 0 3 * * ?" in time zone UTC that calls a use case reading the time from the Clock of architecture-conventions §5.3. One run, in one transaction, deletes in this order: (1) every sessions row, account and portal sessions alike, more than 30 days after it expired, where it expired at the earlier of idle_expires_at and absolute_expires_at; revoked_at does not count, because UD-19.j deletes sessions 30 days after they expired, so a revoked session stays until 30 days after its own expiry time; (2) every auth_attempts row more than 30 days after its 300-second throttle window ended, so with at more than 30 days and 300 seconds before the run; (3) every dj_invites row more than 90 days after it closed, where it closed at the earliest of accepted_at, revoked_at and expires_at; (4) every tasks row, weddings row and users row whose deleted_at is more than 90 days before the run; a wedding takes the rows of its cascades with it, as deleteWedding with purge true does (P1-E04-T05), and an account takes its sessions and memberships, while team slots, vendor contacts, tasks, weddings and portals that name it keep their row with that column set to null. An account still named as invited_by of a dj_invites row stays until that invite is deleted, because invited_by has no on-delete rule in the identity schema. Revoked portals are kept while their wedding exists, because rotateWeddingPortal can switch them back on (P1-E06-T03); archived vendors are not soft-deleted and stay. The rows of audit_log of P1-E09-T10 are never deleted. The job logs one line per run with the number of rows deleted per table and no other value (UD-19.f). Dead schema (15 R-13): the phase-1 schema holds no song_requests table, because no phase-1 operation writes it.

- STOP (human approval in the pull request): delete-history, crypto-logging
- Covers: RISK-35, UD-19.j

Plan item `P1-E09-T09` (technical,P1,stop-delete-history,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the application started When the test lists its scheduled methods Then exactly one exists, the cleanup job, with the cron "0 0 3 * * ?" in time zone UTC (deviation UD-19.j, RISK-35; rekord-api schedules none).
- [ ] #2 Given a fixed clock at 2027-07-01T03:00:00Z and three account sessions, the first with idle_expires_at 2027-05-31T02:00:00Z, the second with absolute_expires_at 2027-06-02T03:00:00Z and idle_expires_at 2027-06-05T03:00:00Z, and the third with revoked_at 2027-05-30T10:00:00Z, idle_expires_at 2027-06-06T10:00:00Z and absolute_expires_at 2027-06-20T10:00:00Z When the cleanup job runs Then the first row is gone and the second and the third row remain, because the third expired at 2027-06-06T10:00:00Z, less than 30 days before the run, and its revoked_at does not count (UD-19.j).
- [ ] #3 Given a fixed clock at 2027-07-01T03:00:00Z and two auth_attempts rows, one with at 2027-05-31T02:00:00Z and one with at 2027-06-01T03:00:00Z When the cleanup job runs Then the first row is gone and the second remains, because its throttle window ended at 2027-06-01T03:05:00Z, less than 30 days before the run (UD-19.j).
- [ ] #4 Given a fixed clock at 2027-07-01T03:00:00Z and five invites, for one.dj@example.com accepted at 2027-04-01T00:00:00Z, for two.dj@example.com revoked at 2027-04-02T00:00:00Z, for three.dj@example.com never accepted with expires_at 2027-04-01T00:00:00Z, for four.dj@example.com accepted at 2027-04-10T00:00:00Z, and for five.dj@example.com open with expires_at 2027-07-08T00:00:00Z When the cleanup job runs Then the first three invites are gone and the last two remain (UD-19.j).
- [ ] #5 Given a fixed clock at 2027-07-01T03:00:00Z, a wedding "Emma & Julian" with deleted_at 2027-04-01T00:00:00Z holding people, team slots, portals, a run-sheet line and a task, a wedding with deleted_at 2027-06-01T00:00:00Z, and a task of the business without a wedding with deleted_at 2027-03-01T00:00:00Z When the cleanup job runs Then the first wedding and all its rows are gone, the second wedding remains with its deleted_at, and the task is gone (UD-19.j).
- [ ] #6 Given a fixed clock at 2027-07-01T03:00:00Z, an account with deleted_at 2027-03-01T00:00:00Z named as user_id on a CONFIRMED team slot with a person_name and on a vendor contact, and named as invited_by of an invite for six.dj@example.com that was never accepted and has expires_at 2027-06-15T00:00:00Z When the cleanup job runs, and runs again with the clock at 2027-09-14T03:00:00Z Then after the first run the account still exists, and after the second run the invite and the account are gone and the slot and the contact remain with user_id null (UD-19.j).
- [ ] #7 Given a wedding without deleted_at whose FRIENDS portal was revoked at 2026-01-01T00:00:00Z and the clock at 2027-07-01T03:00:00Z When the cleanup job runs Then the revoked portal row remains, and rotateWeddingPortal with scope FRIENDS afterwards answers 200 with a live friends link (UD-19.j).
- [ ] #8 Given the cleanup job run of the criteria above When the test reads the log lines it wrote Then there is one line holding the table names and row counts only, and no line holds a token, an access code, an e-mail address or a name (UD-19.f).
- [ ] #9 Given 5 failed sign-ins for planner@example.com from one client, the newest 301 seconds ago When that client calls login with the right password Then the answer is 200 and the 5 rows stay in auth_attempts (RISK-35).
- [ ] #10 Given the phase-1 schema on an empty database When the test lists its tables Then song_requests does not exist (15 R-13).
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
