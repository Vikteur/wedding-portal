---
id: TASK-18.1
title: P2-E03-T01 Add or re-save a song on one of our lists (putPortalEntry)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-17.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:94-121'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:252-274'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:33-94'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:164-215'
  - 'rekord-api/src/main/java/app/rekord/portal/SongMapper.java:76-114'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:59-100'
  - 'docs/rewrite/analysis/12-couple-portal.md:88-91'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-18
priority: high
type: feature
ordinal: 20301
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to add a song to one of our lists or change one already there so that the DJ sees our wish as we meant it.

The one write path of the song lists. Oracle: PortalResource.putPortalEntry and guardWrite (PortalResource.java:94-121, :252-270) and SongService.put and freePosition (SongService.java:33-94, :173-200). An entry is identified by the client-generated uid, unique per wedding; a re-save keeps the position and the source; caps come from the seeded list kinds. Friends write only to friends_top20; rekord-api lets them re-save any friend's row, which the contract text calls append-only (RISK-17, slice 12 R9, BR-ID-48). UD-18.a and UD-19.l make the friends' link add-only. For a FRIENDS session the checks run in this order: a kind other than friends_top20 is refused with 403 FORBIDDEN "This link can only add to the friends' top 20." (as rekord-api); a uid already on friends_top20 whose stored fields equal what the save would store answers 200 with the stored entry unchanged and writes nothing, so a retry after a lost answer succeeds (UD-19.l2); every other uid already stored, whether the couple or a friend saved it and on whatever list, is refused with 403 FORBIDDEN "Friends can add songs but cannot change or remove them." (UD-18.a, UD-19.l1; rekord-api refuses a couple-saved row with 403 FORBIDDEN "That row belongs to the couple." and re-saves a friend-saved row). The fields compared are title, artist, spotify_id, isrc, duration_ms, art_url, free_text and note, after the trimming a save applies; position is not compared, because a re-save never moves an entry. Removing an entry stays couple only (P2-E03-T02). No portal operation writes the list states SUBMITTED or LOCKED (BR-DM-27, UD-18.c). The unchanged friends' view only ever adds a new uid: its song table offers no remove, reorder or replace (rekord-couple/src/GuestApp.tsx:92-99), and its pick calls pickSong without replaceUid (rekord-couple/src/parts.tsx:177), so the only re-save it sends is a retry with identical fields, which UD-19.l2 answers 200; the one caller that re-saves an existing uid is the couple's opening-dance screen (rekord-couple/src/screens.tsx:102), through a COUPLE session. putPortalEntry has rekord-api tests, so it is not built test-first. The song_entries table comes from P2-E02-T01. start_pref is not stored (see P2-E03-T03, UD-18.d).

- Builds: `putPortalEntry`
- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-15, BR-CP-16, BR-CP-17, BR-CP-18, RISK-17, BR-ID-39, BR-ID-48, PIN-10-0318, BR-DM-27, UD-18.a, UD-18.c1, UD-19.l1, UD-19.l2, UD-19.d3
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all (P0-E05); rekord-api raises it on a concurrent first PUT of one uid (ux_song_entries_uid, V6__music.sql:92), which P2-E03-T04 turns into 200; P2-E05-T02 criterion 7 provokes it through a test hook to prove the rollback.

Plan item `P2-E03-T01` (user-story,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and couple_top20 holding entries at positions 0, 1 and 2 When the partner calls putPortalEntry for the new uid u-1 with kind couple_top20, title " Dancing Queen " and artist "ABBA" Then the answer is 200 with entry {uid u-1, title "Dancing Queen", artist "ABBA", position 3, source_token_kind couple} and entries holding the 4 couple_top20 entries by position.
- [ ] #2 Given u-1 stored with title "Dancing Queen", artist "ABBA", note "loud" and position 3 When the partner calls putPortalEntry for u-1 with the body {kind: couple_top20, title: "Waterloo", position: 0} and no other field Then the answer is 200 and u-1 keeps position 3 and source_token_kind couple, with title "Waterloo", artist "" and note, spotify_id, isrc, duration_ms, art_url and free_text null.
- [ ] #3 Given the same session When the partner calls putPortalEntry for a new uid with title "  ", free_text " our song ", artist " ABBA " and a note of 5000 characters, and for another new uid with title "" and free_text "   " Then the first answers 200 and is stored with title "our song", free_text "our song", artist " ABBA " and the note exactly as sent, and the second answers 400 EMPTY_TITLE "Pick a song or type one in first." and stores nothing.
- [ ] #4 Given must_plays holding entries at positions 0 and 2 When the partner calls putPortalEntry for 3 new uids requesting position 2, position 7 and no position, and once more for a 4th new uid Then the 3 entries land at positions 1, 3 and 4, and the 4th answers 409 LIST_FULL "Must-plays, no matter what is full — all 5 spots are taken." and is not stored.
- [ ] #5 Given playlist_links holding entries at positions 0 and 4 When the partner calls putPortalEntry for a new uid requesting position 1 Then it lands at position 5, because an uncapped list appends after its highest position.
- [ ] #6 Given u-1 on couple_top20 When the partner calls putPortalEntry for u-1 with kind must_plays Then the answer is 409 UID_CONFLICT "That song is already on another list." and u-1 stays on couple_top20.
- [ ] #7 Given a FRIENDS session of the same wedding and u-1 saved by the couple When the friend calls putPortalEntry with kind must_plays, next for u-1 with kind friends_top20, and next for the new uid f-1 with kind friends_top20 Then the answers are 403 FORBIDDEN "This link can only add to the friends' top 20.", 403 FORBIDDEN "Friends can add songs but cannot change or remove them." with u-1 unchanged, and 200 with f-1 stored with source_token_kind friend and entries holding only friends_top20 (deviation UD-18.a/UD-19.l1 for the second; rekord-api answers it 403 FORBIDDEN "That row belongs to the couple.").
- [ ] #8 Given a FRIENDS session, f-1 saved by a friend with title "Waterloo", artist "ABBA" and note "loud" at position 1, and u-2 saved on friends_top20 by the couple with title "Fernando" and artist "ABBA" When a friend repeats f-1 with title " Waterloo ", artist "ABBA", note "loud" and position 0, and sends u-2 with title "Fernando" and artist "ABBA", both with kind friends_top20 Then both answer 200 with entry equal to the stored entry (same uid, fields, position, created_at and updated_at) and entries holding only friends_top20, nothing is written and no song_changes row is added (deviation UD-19.l2; rekord-api re-saves f-1 and refuses u-2 with 403 FORBIDDEN "That row belongs to the couple.").
- [ ] #9 Given a FRIENDS session and f-1 saved on friends_top20 through the friends' link with title "Waterloo" When one friend's device calls putPortalEntry for f-1 with kind friends_top20 and title "Other song", and a second device with the same FRIENDS link calls it for f-1 with kind friends_top20, title "Waterloo" and note "louder" Then both answer 403 FORBIDDEN "Friends can add songs but cannot change or remove them." and f-1 keeps every field, its position and its source friend (deviation UD-18.a and UD-19.l1; rekord-api lets any friend re-save a friend's row).
- [ ] #10 Given a wedding whose opening_dance list is WAITING with song_count 0 When the partner calls putPortalEntry for its first entry Then the list has song_count 1 and state IN_PROGRESS, and no portal operation of this phase writes the states SUBMITTED or LOCKED or their timestamps (BR-DM-27).
- [ ] #11 Given a test fixture wedding with no must_plays list row When the partner calls putPortalEntry with kind must_plays Then the answer is 404 UNKNOWN_ENTRY "There is no such list on this wedding.".
- [ ] #12 Given the same wedding When putPortalEntry is called without a cookie, with another wedding's token and this wedding's cookie, with a 10-character token, and by the COUPLE session with a body without kind, with {kind "dance", title "Waterloo"}, with {kind "couple_top20", title "Waterloo", position "first"} and with the text "not json" as application/json Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK, 422 VALIDATION_FAILED, and 422 VALIDATION_FAILED with errors exactly {field kind, code REQUIRED}, {field kind, code INVALID_VALUE}, {field position, code INVALID_FORMAT} and {field null, code INVALID_FORMAT}, and nothing is stored (deviation UD-19.d3 for the last three).
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
