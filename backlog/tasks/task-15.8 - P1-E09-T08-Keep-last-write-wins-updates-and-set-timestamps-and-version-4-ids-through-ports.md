---
id: TASK-15.8
title: >-
  P1-E09-T08 Keep last-write-wins updates and set timestamps and version-4 ids
  through ports
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
milestone: m-1
dependencies:
  - TASK-10.5
  - TASK-11.2
  - TASK-14.3
  - TASK-9.3
  - TASK-13.2
  - TASK-10.4
references:
  - 'docs/rewrite/analysis/15-data-model-persistence.md:105'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:525'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:166-171'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:56'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:207-212'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:235-239'
  - 'rekord-contract/components/common.yaml:136-142'
  - 'docs/rewrite/analysis/11-planner-weddings.md:292'
  - 'docs/rewrite/architecture-conventions.md:362'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10908
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the phase-1 writes to set timestamps and ids through ports with the update rules of rekord-api so that tests control time and ids and no answer changes.

Lost updates: no phase-1 entity except song_lists carries a version column; song_lists keeps the version column and @Version of the oracle (SongList.java:27), and no phase-1 operation updates a song list. No operation checks an ETag or version, so the last write wins, as in rekord-api (RISK-27, BR-PL-57); the user kept last-write-wins with no version check (UD-19.h). created_by is stored on weddings, portals and tasks and completed_by on tasks (BR-PL-57). created_at and updated_at of every phase-1 row are set by the application from the java.time.Clock of architecture-conventions §5.3 on insert, and updated_at on every entity update (BR-DM-39, PIN-15-0105). The bulk updates of rekord-api keep their rule: the partner stale flag of updatePerson and the portal revoke of a soft delete leave the portals' updated_at as it was, and the stale flag of a date move sets it (WeddingsResource.java:166-171, WeddingService.java:207-212, :235-239). Ids are issued from the IdGenerator port as random UUIDs of version 4, as rekord-api does (WeddingService.java:56; architecture-conventions.md:678; BR-PL-56).

- Covers: RISK-27, BR-PL-57, BR-DM-39, PIN-15-0105, BR-PL-56, UD-19.h

Plan item `P1-E09-T08` (technical,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner and a fixed test clock at 2027-06-01T10:00:00Z When the planner calls createWedding Then the wedding, its people, team slots, song lists and portals hold created_at and updated_at 2027-06-01T10:00:00Z, and the wedding and its portals hold created_by the planner (BR-DM-39, BR-PL-57).
- [ ] #2 Given that wedding and the clock moved to 2027-06-02T09:00:00Z When the planner calls updateWedding with a new note Then the wedding's updated_at is 2027-06-02T09:00:00Z and its created_at is unchanged (PIN-15-0105).
- [ ] #3 Given two planners who read the same task When the first calls updateTask with title "Book the florist" and then the second with title "Call the florist" Then both answer 200 and the task holds "Call the florist" (RISK-27, BR-PL-57, UD-19.h).
- [ ] #4 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and the clock moved on When the planner calls updatePerson on the PARTNER Julian with given_name "Noah" Then both portals have access_code_stale true and keep their former updated_at (BR-DM-39).
- [ ] #5 Given that wedding and the clock moved on When the planner calls updateWedding with wedding_date 2027-07-03 Then both portals have access_code_stale true and updated_at equal to the clock (BR-DM-39).
- [ ] #6 Given that wedding and the clock moved on When the planner calls deleteWedding without purge Then both portals have revoked_at set and keep their former updated_at (BR-DM-39).
- [ ] #7 Given a planner When the planner calls createWedding, createVendor, addTimelineItem and createTask Then every id in the answers is a UUID whose version digit is 4 (BR-PL-56).
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
