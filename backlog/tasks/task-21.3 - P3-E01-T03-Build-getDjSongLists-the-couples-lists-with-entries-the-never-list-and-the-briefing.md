---
id: TASK-21.3
title: >-
  P3-E01-T03 Build getDjSongLists: the couple's lists with entries, the never
  list and the briefing
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-21.2
references:
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:79-115'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:142-150'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:256-263'
  - 'rekord-contract/paths/dj.yaml:55-86'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:178'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-21
priority: high
type: feature
ordinal: 30103
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to read every song the couple and their friends chose, the songs they never want and their briefing so that I build the night around their wishes.

The DJ is one of the two parties the music is for, so this answer carries entries rather than counts. rekord-api returns every song list except `never` with its entries ordered by position, and the never list on its own as `blocklist`, because it is a filter and not a playlist (BR-MX-43). The briefing text is the couple's own words about the night (P2). Visibility is the assignment rule of P3-E01-T01.

- Builds: `getDjSongLists`
- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-43, BR-CP-27, BR-CP-38, UD-18.c2
- Error code `FORBIDDEN` without a criterion here: answered by the account check of the use case with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} to a session that carries no member account, such as a couple portal session, as rekord-api answers (AppIdentity.java:81-87, ErrorMappers.java:67-73); asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E01-T03` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an assigned wedding whose couple_top20 list holds 3 entries at positions 2, 0 and 1 and whose never list holds 2 entries When the DJ calls getDjSongLists Then the answer is 200, song_lists holds every list of the wedding except never in the song-list kinds' sort order, the couple_top20 entries come in position order 0, 1, 2, and blocklist holds the 2 never entries as BlockEntry items.
- [ ] #2 Given an assigned wedding whose briefing_text is "Start slow, end loud" When the DJ calls getDjSongLists Then briefing_text is "Start slow, end loud", and for a wedding without a briefing it is null.
- [ ] #3 Given an assigned wedding whose couple_top20 list is in state IN_PROGRESS and holds 3 entries When the DJ calls getDjSongLists Then that list carries kind couple_top20, label "Their top 20" (the kind's label), order 3 (the kind's sort_order), max_songs 20 (the kind's max_songs), state IN_PROGRESS and song_count 3, and no list in song_lists has the kind never (WeddingMapper.java:159-174, R__seed_song_list_kinds.sql, CouplesResource.java:79-115).
- [ ] #4 Given a wedding the DJ is not assigned to, a wedding of another business and an unknown id When the DJ calls getDjSongLists with each Then each answers 404 NO_WEDDING with the message "There is no such wedding.".
- [ ] #5 Given a member holding only the ADMIN role who fills no slot of a wedding of the business When the admin calls getDjSongLists for it Then the answer is 200 with the same content an assigned DJ gets (deviation UD-14.b3; rekord-api answers 404 NO_WEDDING).
- [ ] #6 Given a wedding whose briefing_text is "Start slow, end loud" and a member holding only the PLANNER role who fills no slot of it When the planner calls getDjSongLists for it Then the answer is 404 NO_WEDDING, so a planner off the team never reads the couple's briefing or songs (BR-CP-27; WeddingRepository.java:96-123).
- [ ] #7 Given an assigned wedding whose couple_top20 list holds 3 entries and which the DJ has loaded with getDjSongLists When the couple saves a fourth entry with putPortalEntry and the DJ calls getDjSongLists again Then couple_top20 holds 4 entries with song_count 4 without any submit step in between, and no list in song_lists has the state SUBMITTED or LOCKED (deviation UD-18.c; rekord-api has the list states SUBMITTED and LOCKED).
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
