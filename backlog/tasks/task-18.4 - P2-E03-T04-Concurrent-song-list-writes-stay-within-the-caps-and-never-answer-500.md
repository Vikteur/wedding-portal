---
id: TASK-18.4
title: >-
  P2-E03-T04 Concurrent song-list writes stay within the caps and never answer
  500
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - technical
  - P2
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-18.1
  - TASK-18.2
  - TASK-18.3
references:
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:33-140'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:202-215'
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:109-121'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:97-100'
  - 'docs/rewrite/analysis/12-couple-portal.md:518'
  - 'rekord-api/src/main/java/app/rekord/domain/SongList.java:27'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:92'
  - 'docs/rewrite/analysis/40-api-compatibility-matrix.md:91'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-18
priority: high
type: task
ordinal: 20304
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want concurrent writes to a song list to keep the cap and the positions intact so that two devices of one couple never corrupt a list.

rekord-api does not lock the list on a write: two concurrent new entries can exceed a cap, and conflicts on the optimistic lock of the recount or on the deferred unique position surface as 500 UNKNOWN (RISK-37, slice 12 R6, ErrorMappers.java:109-121). The rewrite action of slice 12 R6 asks for conflicts to become 409. These criteria are the concurrency guarantees of putPortalEntry, deletePortalEntry and reorderPortalList built in the tickets before this one. SongList carries a version column as in rekord-api (BR-DM-40, V6 music schema line 51); two writes to one list are serialised: putPortalEntry, deletePortalEntry and reorderPortalList lock their song_lists row (SELECT … FOR UPDATE) before the domain checks (architecture-conventions §8.1, FW-C-39), so the second write runs its checks after the first commits, each gets the answer it would get if the two were sent one after the other, and the database never refuses either write (the race policy of P0-E05-T05); criteria 1 to 7 pin it, criterion 7 for two FRIENDS sessions, whose second write meets the add-only rule of UD-18.a and UD-19.l.

- STOP (human approval in the pull request): auth-access
- Covers: RISK-37, PIN-12-0518, BR-DM-40, UD-18.a, UD-19.l1, UD-19.l2

Plan item `P2-E03-T04` (technical,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given must_plays holding 4 of its 5 entries When two putPortalEntry calls for 2 new uids arrive at the same time Then one answers 200, the other answers 409 LIST_FULL, and the list holds exactly 5 entries with song_count 5 (rekord-api can store 6 or answer 500).
- [ ] #2 Given an empty couple_top20 When two putPortalEntry calls for 2 new uids both requesting position 0 arrive at the same time Then both answer 200 and the entries are at positions 0 and 1.
- [ ] #3 Given u-1 on couple_top20 When two putPortalEntry re-saves of u-1 from two COUPLE sessions with titles "A" and "B" arrive at the same time Then both answer 200 and u-1 holds the title of the call that committed last.
- [ ] #4 Given couple_top20 holding u-a at 0 and u-b at 1 When reorderPortalList with [u-b 0, u-a 1] and putPortalEntry for the new uid u-c arrive at the same time Then either the reorder answers 200 with u-b at 0 and u-a at 1 and the put answers 200 with u-c at 2, or the put answers 200 with u-c at 2 first and the reorder answers 422 VALIDATION_FAILED naming u-c as missing with no position changed; positions 0 to 2 each hold one entry and no answer is 500 UNKNOWN.
- [ ] #5 Given couple_top20 holding u-1 to u-10 at positions 0 to 9 When 10 putPortalEntry calls for the new uids n-1 to n-10, 5 deletePortalEntry calls for u-1 to u-5 and 5 reorderPortalList calls each sending u-1 to u-10 in reverse order start at the same time Then every answer is 200, 404 UNKNOWN_ENTRY, 409 LIST_FULL or 422 VALIDATION_FAILED and none is 500 UNKNOWN, song_count equals the number of entries, and no two entries share a position.
- [ ] #6 Given an empty couple_top20 When two putPortalEntry calls from two COUPLE sessions for the same new uid u-n with titles "A" and "B" arrive at the same time Then both answer 200, couple_top20 holds exactly one entry u-n with song_count 1 and the title of the call that committed last, and no answer is 500 UNKNOWN (rekord-api can answer 500 UNKNOWN on ux_song_entries_uid, 40-api-compatibility-matrix.md:91).
- [ ] #7 Given an empty friends_top20 When two FRIENDS sessions send putPortalEntry for the same new uid f-n at the same time, in a first run with titles "A" and "B" and in a second run both with title "A" and no other field Then in the first run one answers 200 and the other 403 FORBIDDEN "Friends can add songs but cannot change or remove them.", in the second both answer 200 with an equal entry f-n (same fields, created_at and updated_at), and in each run friends_top20 holds exactly one entry f-n with song_count 1 and the title of the call answered 200 in the first run and "A" in the second, one song_changes row is added and no answer is 500 UNKNOWN (UD-18.a, UD-19.l1, UD-19.l2).
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
