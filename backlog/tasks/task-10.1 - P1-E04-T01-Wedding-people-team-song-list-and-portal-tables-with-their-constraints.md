---
id: TASK-10.1
title: >-
  P1-E04-T01 Wedding, people, team, song-list and portal tables with their
  constraints
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - technical
  - P1
  - stop-db-migration
  - stop-crypto-logging
milestone: m-1
dependencies:
  - TASK-9.1
  - TASK-7.2
references:
  - rekord-api/src/main/resources/db/migration/V4__weddings.sql
  - rekord-api/src/main/resources/db/migration/V5__portals.sql
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:13-57'
  - >-
    rekord-api/src/main/resources/db/migration/V11__relax_assignment_constraints.sql:25-44
  - rekord-api/src/main/resources/db/migration/R__seed_song_list_kinds.sql
  - 'docs/rewrite/analysis/15-data-model-persistence.md:55-104'
  - 'docs/rewrite/analysis/11-planner-weddings.md:92'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-10
priority: high
type: task
ordinal: 10401
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the wedding tables in a Flyway migration of wedding-portal so that a wedding, its people, its team slots, its song lists and its two portals have a schema with the oracle's constraints.

Oracle: V4__weddings.sql (weddings, wedding_people, wedding_team with their indexes), the two checks of V11__relax_assignment_constraints.sql, V5__portals.sql (couple_portals), the song_list_kinds and song_lists tables of V6__music.sql and the repeatable seed R__seed_song_list_kinds.sql. Kept as in the oracle: every column, the venue check in its V11 form (a COMPLETED wedding names a venue vendor or a venue name, BR-DM-17), ux_weddings_slug over business and slug among rows with deleted_at null (BR-DM-16), the partner-slot index (BR-DM-18), the team checks ck_wedding_team_contact and ck_wedding_team_assigned in its V11 form and ux_wedding_team_role (BR-DM-19), ux_song_lists (BR-DM-25), the global ux_couple_portals_token and the partial ux_couple_portals_live (BR-PL-18), and the session foreign keys fk_sessions_wedding and fk_sessions_portal that the oracle adds to the sessions table of P1-E01-T02. The seed upserts the 7 kinds on conflict and never deletes one (BR-DM-05). Three deliberate deviations: couple_portals has no expires_at column, because the end of a link is computed from the current wedding date and nothing about it is stored (UD-9, UD-10.c); and access_code is nullable, because a portal whose code cannot be derived stores no code instead of the literal PENDING (RISK-02); and the never kind does not count toward progress (UD-18.c). The portal token and the access code stay readable in the table, not hashed, for the reason the V5 header gives: the planner reads both back to the couple (BR-DM-21, RISK-07), and the user kept this (UD-19.h). briefing_text is phase 3, song_entries and the portal attempt tables are phase 2. Repository tests run on Testcontainers PostgreSQL (P0-E04-T02).

- STOP (human approval in the pull request): db-migration, crypto-logging
- Covers: BR-DM-05, BR-DM-06, BR-DM-16, BR-DM-17, BR-DM-18, BR-DM-19, BR-DM-25, BR-DM-21, BR-PL-18, BR-CP-08, RISK-07, PIN-15-0077, PIN-15-0078, PIN-15-0079, PIN-15-0080, PIN-15-0486, UD-19.h

Plan item `P1-E04-T01` (technical,P1,stop-db-migration,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the migrations of P1-E01 to P1-E03 When wedding-portal starts on an empty database Then Flyway applies the wedding migration and the repeatable seed, the tables weddings, wedding_people, wedding_team, song_list_kinds, song_lists and couple_portals exist with the oracle's columns, couple_portals has no expires_at column and a nullable access_code, and Hibernate validation passes.
- [ ] #2 Given the started database When the schema test reads song_list_kinds Then it holds the 7 kinds opening_dance, second_third, couple_top20, friends_top20, never, must_plays and playlist_links with sort_order 1 to 7, max_songs 1, 2, 20, 20, null, 5 and null, is_blocklist true only for never, friends_writable true only for friends_top20, and counts_toward_progress false for never and playlist_links (PIN-15-0486; deviation UD-18.c; rekord-api seeds never as counting).
- [ ] #3 Given the seeded kinds with the label of never changed by hand to "x" When the repeatable seed runs again Then the table still holds exactly 7 rows and never has the label "The never list" again (BR-DM-05).
- [ ] #4 Given a wedding row When the repository test sets its status to COMPLETED with neither venue_vendor_id nor venue_name_override, then with venue_name_override "De Oude Tuinderij", then inserts a CONFIRMED wedding without either Then the first update fails on ck_weddings_venue and the second update and the insert succeed (BR-DM-17, PIN-15-0078).
- [ ] #5 Given a wedding with PARTNER rows at sort_order 0 and 1 When the repository test inserts a third PARTNER row with sort_order 1 and a FAMILY row with sort_order 1 Then the PARTNER insert fails on ux_wedding_people_partner_slot and the FAMILY insert succeeds (BR-DM-18, PIN-15-0079).
- [ ] #6 Given a wedding with a DJ slot at sort_order 0 When the repository test inserts a second DJ slot at sort_order 0, a slot with a vendor_contact_id and no vendor_id, a CONFIRMED slot with no vendor_id, user_id or person_name_override, and a CONFIRMED slot with only a user_id Then the first three inserts fail on ux_wedding_team_role, ck_wedding_team_contact and ck_wedding_team_assigned, and the fourth succeeds (BR-DM-19, PIN-15-0080).
- [ ] #7 Given a live wedding with slug emma-julian When the repository test inserts a second live wedding of the same business with that slug, the same slug after the first got deleted_at, and the same slug in another business Then only the first insert fails on ux_weddings_slug (BR-DM-16).
- [ ] #8 Given a wedding with a song list of kind never When the repository test inserts a second never list for that wedding Then the insert fails on ux_song_lists (BR-DM-25).
- [ ] #9 Given a wedding with a live COUPLE portal When the repository test inserts a second live COUPLE portal, then the same insert after the first portal got revoked_at, then a portal of another business with the first portal's token, then a FRIENDS portal with access_code null Then the first fails on ux_couple_portals_live, the second succeeds, the third fails on ux_couple_portals_token and the fourth succeeds (BR-PL-18, BR-CP-08).
- [ ] #10 Given a portal inserted with a 43-character token and the access code EJ12062027 When the repository test reads the row back Then the token and the code columns hold exactly those two values (BR-DM-21, RISK-07).
- [ ] #11 Given a wedding with people, team slots, song lists, two portals and a PORTAL session row on its COUPLE portal When the repository test deletes the wedding row, and in a second run deletes its business row Then every one of those rows is gone in both runs (BR-DM-06).
- [ ] #12 Given the tables of this migration When the schema test reads their foreign keys Then weddings, wedding_people, wedding_team, song_lists and couple_portals each have org_id not null referencing organizations with ON DELETE CASCADE, and song_list_kinds has no org_id (BR-DM-06).
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
