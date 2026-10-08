---
id: TASK-22.1
title: >-
  P3-E02-T01 Library schema with global tracks, source claims, playlists,
  preferences and settings
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - technical
  - P3
  - stop-db-migration
milestone: m-3
dependencies:
  - TASK-4
references:
  - 'rekord-api/src/main/resources/db/migration/V8__library.sql:1-148'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:116'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:92'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: task
ordinal: 30201
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the library tables created by one new Flyway migration so that every library, matching and export ticket of this phase builds on the schema rekord-api proved.

wedding-portal owns its database from V1 (P0-E04) and each ticket creates the tables it needs, with rekord-api's migrations as the oracle schema. The library half is V8__library.sql of rekord-api. Two of its choices are deliberate and must survive: tracks are global, one row per absolute path with no org_id, and a library holds a track only through a claim of one of its sources (BR-LIB-10, the tenancy exception at V8__library.sql:19-26); imported playlist rows and remembered choices keep a track id without a foreign key, so they outlive a file's absence and rebind when it returns. There is no data import (UD-13).

- STOP (human approval in the pull request): db-migration
- Covers: BR-LIB-10

Plan item `P3-E02-T01` (technical,P3,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an empty PostgreSQL database in a Testcontainers test When wedding-portal starts Then one new Flyway migration has created the tables libraries, sources, tracks, track_sources, rekordbox_playlists, rekordbox_playlist_tracks, preferences and settings with the columns, types, checks, defaults and foreign keys of rekord-api's V8__library.sql:28-148, and the test lists these 8 tables.
- [ ] #2 Given the migrated schema When the test reads the tracks table Then it has no org_id column, its primary key is the text id, path is unique, and tag_source accepts only tags, filename and rekordbox (BR-LIB-10).
- [ ] #3 Given the migrated schema When the test inserts two libraries of one business for one owner named "Studio" and "studio" Then the second insert fails on the unique index over (org_id, owner_id, lower(name)), and two unowned libraries of one business named "Old" and "OLD" fail on the partial unique index over (org_id, lower(name)) where owner_id is null.
- [ ] #4 Given the migrated schema When the test inserts two sources with the same library, kind and label, or two rekordbox playlists of one library whose names differ only in letter case Then each second insert fails on its unique index (ux_sources, ux_rekordbox_playlists).
- [ ] #5 Given a library with a source, a claim, an imported playlist with 2 track rows and a preference When the test deletes the library row Then the source, the claim, the playlist, its track rows and the preference are gone by cascade, and the tracks row stays.
- [ ] #6 Given a preference and a playlist track row that name a track id with no tracks row When the test inserts them Then both inserts succeed, because neither column has a foreign key to tracks (V8__library.sql:106-133).
- [ ] #7 Given the migrated schema When the test inserts two settings rows with the same org_id, user_id and key Then the second insert fails on the primary key (org_id, user_id, key).
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
