---
id: TASK-13.1
title: >-
  P1-E07-T01 Create the timeline_items table with the one-owner and day-offset
  checks
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - technical
  - P1
  - stop-db-migration
milestone: m-1
dependencies:
  - TASK-10.1
references:
  - 'rekord-api/src/main/resources/db/migration/V7__planning.sql:1-38'
  - rekord-api/src/main/java/app/rekord/domain/TimelineItem.java
  - 'docs/rewrite/STATUS.md:282-284'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-13
priority: high
type: task
ordinal: 10701
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the timeline_items table and the TimelineItem entity so that the run-sheet operations of P1-E07 have their storage with the database backstops of rekord-api.

Creates the table timeline_items as the oracle defines it (V7__planning.sql:3-38) in a new Flyway migration of wedding-portal: id uuid, org_id and wedding_id not null with on delete cascade, day_offset smallint not null default 0 checked between 0 and 1, at_time time not null, duration_min integer, what text not null, owner_team_id referencing wedding_team and owner_person_id referencing wedding_people, both on delete set null, owner_role checked against VENUE, DJ, CATERING, PHOTO, MC and OTHER, visibility not null default ALL checked against ALL, TEAM and PLANNER_ONLY, sort_order smallint not null default 0, created_at and updated_at, the constraint ck_timeline_one_owner (at most one of the two owner ids set) and the index ix_timeline_items on (wedding_id, day_offset, at_time, sort_order). Adds the TimelineItem entity in the persistence adapter with the fields of TimelineItem.java. The checks stay as a backstop; the use cases of P1-E07-T02 to P1-E07-T04 refuse the same values with 422 before they reach the database.

- STOP (human approval in the pull request): db-migration

Plan item `P1-E07-T01` (technical,P1,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the migrations of P1-E04-T01 When wedding-portal starts on an empty database Then Flyway applies the timeline migration, timeline_items has the columns, defaults, checks, foreign keys, the constraint ck_timeline_one_owner and the index ix_timeline_items of V7__planning.sql:3-38, and Hibernate validation of TimelineItem passes.
- [ ] #2 Given a wedding When the repository test inserts a line with day_offset 2, a line with owner_role BAND, a line with visibility PRIVATE, and a line with both owner_team_id and owner_person_id set Then each insert fails, on the day_offset check, the owner_role check, the visibility check and ck_timeline_one_owner in turn.
- [ ] #3 Given a wedding with a line owned by a person and a line owned by a team slot When the test deletes the person row and the team slot row Then both lines remain with owner_person_id and owner_team_id null (on delete set null).
- [ ] #4 Given a wedding with 3 lines When the wedding row is deleted in the test Then the 3 lines are gone (on delete cascade), and a line inserted without visibility, day_offset and sort_order holds ALL, 0 and 0.
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
