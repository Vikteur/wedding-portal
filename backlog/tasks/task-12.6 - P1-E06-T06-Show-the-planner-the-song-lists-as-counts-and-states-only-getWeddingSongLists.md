---
id: TASK-12.6
title: >-
  P1-E06-T06 Show the planner the song lists as counts and states only
  (getWeddingSongLists)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/music/MusicResource.java:16-50'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:256-263'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:159-199'
  - 'rekord-contract/paths/planner.yaml:378-411'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:349-358'
  - rekord-api/src/main/resources/db/migration/R__seed_song_list_kinds.sql
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-12
priority: high
type: feature
ordinal: 10606
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to see which song lists the couple filled and how full they are so that I know where the music stands without reading their songs.

Builds the operation getWeddingSongLists. Oracle: MusicResource.getWeddingSongLists (MusicResource.java:40-50) with WeddingService.songListsOf (WeddingService.java:256-263) and WeddingMapper.summaries and progress (WeddingMapper.java:159-199). The answer is SongListSummaries: song_lists, one SongListSummary per list ordered by the kind's sort order with kind, label, order, state, song_count and max_songs and no field that could hold a song, and progress, counting the kinds whose counts_toward_progress is true and, of those, the lists that hold a song (UD-18.c: no list is ever SUBMITTED or LOCKED) (BR-PL-05, BR-CP-25, BR-ID-24). The operation is open to every signed-in member: the wedding is found with the visibility rule of BR-PL-03, so a DJ named on a PENCILLED or CONFIRMED slot reads it and anyone else who does not see the wedding gets 404 NO_WEDDING "There is no such wedding.". lists_total is 5, because the never list does not count toward progress (deviation UD-18.c; rekord-api counts it, lists_total 6).

- Builds: `getWeddingSongLists`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-05, BR-CP-25, BR-ID-24
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E06-T06` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 just created When a planner calls getWeddingSongLists Then the answer is 200 with 7 song_lists ordered opening_dance, second_third, couple_top20, friends_top20, never, must_plays, playlist_links, each with its seeded label, order 1 to 7, state WAITING, song_count 0 and max_songs 1, 2, 20, 20, null, 5 and null, and progress lists_in 0 and lists_total 5 (deviation UD-18.c).
- [ ] #2 Given that wedding whose couple_top20 list holds 3 songs, whose must_plays list holds 1 song, whose never list holds 2 songs and whose playlist_links list holds 4 songs When a planner calls getWeddingSongLists Then progress is lists_in 2 and lists_total 5, and couple_top20 shows song_count 3 (deviation UD-18.c).
- [ ] #3 Given that wedding with songs on its lists When a planner calls getWeddingSongLists Then no song_lists item has an entries field and the answer holds no song title, artist or uid (BR-PL-05, BR-CP-25, BR-ID-24).
- [ ] #4 Given that wedding with a DJ member named on its DJ slot as PENCILLED, and a second DJ member named on no slot When each DJ calls getWeddingSongLists Then the first answer is 200 with the 7 lists and the second is 404 NO_WEDDING "There is no such wedding." (BR-PL-03).
- [ ] #5 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls getWeddingSongLists with each Then each answer is 404 NO_WEDDING "There is no such wedding.".
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
