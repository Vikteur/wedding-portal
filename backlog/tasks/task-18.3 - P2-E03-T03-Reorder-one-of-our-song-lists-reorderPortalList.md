---
id: TASK-18.3
title: P2-E03-T03 Reorder one of our song lists (reorderPortalList)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - test-first
  - stop-auth-access
  - stop-contract-push
milestone: m-2
dependencies:
  - TASK-18.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:139-155'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:113-140'
  - 'rekord-couple/src/store.tsx:239-260'
  - 'rekord-contract/components/music.yaml:345-364'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:97-100'
  - 'docs/rewrite/analysis/12-couple-portal.md:93'
  - 'docs/rewrite/analysis/12-couple-portal.md:108'
  - 'docs/rewrite/analysis/12-couple-portal.md:518'
  - 'rekord-contract/components/music.yaml:209-214'
  - 'rekord-contract/components/music.yaml:312-316'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-18
priority: high
type: feature
ordinal: 20303
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to put the songs of a list in a new order so that the DJ knows which matter most.

Reordering a list. rekord-api has no test for it, so it is built test-first (UD-15.d). Oracle: PortalResource.java:139-155 sorts the request by position (a stable sort) and SongService.reorder (SongService.java:113-140) renumbers the named entries 0 to n-1; entries left out keep their old slot, which can collide with the deferred unique position and fail with 500 (slice 12 R6, P8). The couple app sends the whole list sorted by position, possibly with gaps (rekord-couple/src/store.tsx:239-260), so gaps are renumbered, never refused. A request that leaves an entry out or names a uid twice is refused under UD-12 instead of failing. UD-18.d removes start_pref from SongEntry (rekord-contract/components/music.yaml:165, :209-214) and EntryInput (:312-316); wedding-portal ignores an unknown start_pref property on putPortalEntry instead of answering 422, and remarks for the DJ use note. UD-18.a and UD-19.l1: a reorder changes the position of existing entries, so every reorderPortalList call of a FRIENDS session is refused with 403 FORBIDDEN "Friends can add songs but cannot change or remove them." instead of rekord-api's 403 FORBIDDEN "That part belongs to the couple.".

- Builds: `reorderPortalList`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access, contract-push
- Covers: BR-CP-20, BR-CP-35, BR-DM-26, UD-18.d
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is what rekord-api answers when a reorder leaves an entry out (slice 12 P8); wedding-portal refuses that request with 422 VALIDATION_FAILED instead (criterion 4), and P2-E03-T04 proves no reorder answers 500.

Plan item `P2-E03-T03` (user-story,P2,test-first,stop-auth-access,stop-contract-push) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session and couple_top20 holding u-a at 5, u-b at 9 and u-c at 0 When the partner calls reorderPortalList for couple_top20 with positions [u-c 0, u-a 1, u-b 2], first against rekord-api as a characterization test and then against wedding-portal Then the answer is 200 with entries showing u-c at 0, u-a at 1 and u-b at 2.
- [ ] #2 Given couple_top20 holding u-a at 0 and u-b at 3 When the partner calls reorderPortalList with positions [u-b 0, u-a 3] Then u-b is at 0 and u-a at 1, because the supplied numbers order the entries and are not kept.
- [ ] #3 Given couple_top20 holding u-a at 0 and u-b at 1, and u-x on must_plays When the partner calls reorderPortalList for couple_top20 with [u-a 0, u-b 1, u-z 2], and with [u-a 0, u-b 1, u-x 2] Then both answer 404 UNKNOWN_ENTRY "That song is not on this list." and no position changes.
- [ ] #4 Given couple_top20 holding u-a at 0, u-b at 1 and u-c at 2 When the partner calls reorderPortalList with only [u-c 0], and with [u-a 0, u-a 1, u-b 2, u-c 3] Then both answer 422 VALIDATION_FAILED with one errors item per violation, the first message naming u-a and u-b as missing and the second naming u-a as repeated, and no position changes (deviation slice 12 R6 and P8 under UD-12; rekord-api answers 500 UNKNOWN to the first, and 200 to the second, leaving u-a at 0, u-b at 2 and u-c at 3).
- [ ] #5 Given couple_top20 with u-a, u-b and u-c and song_count 3 When the partner reorders it with all 3 uids Then song_count stays 3 and the list state is unchanged.
- [ ] #6 Given couple_top20 holding u-a and u-b at the same requested position 0 in the request [u-a 0, u-b 0] When the partner calls reorderPortalList Then u-a is at 0 and u-b at 1, keeping the request order for equal positions.
- [ ] #7 Given the partner sent start_pref chorus and note "Start at the chorus" for u-s on opening_dance through putPortalEntry, which answered 200 When the partner reorders opening_dance and calls getPortalState Then both answers are 200, u-s holds note "Start at the chorus", and no entry carries a start_pref field (deviation UD-18.d; rekord-api stores and returns start_pref).
- [ ] #8 Given a FRIENDS session of the same wedding When the friend calls reorderPortalList for friends_top20 Then the answer is 403 FORBIDDEN "Friends can add songs but cannot change or remove them." and no position changes (deviation UD-18.a/UD-19.l1; rekord-api answers 403 FORBIDDEN "That part belongs to the couple.").
- [ ] #9 Given the same wedding When reorderPortalList is called without a cookie, with another wedding's token and this wedding's cookie, with a 10-character token, with a body without positions, and with a positions item without uid Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK, 422 VALIDATION_FAILED, 422 VALIDATION_FAILED and 422 VALIDATION_FAILED, in that order.
- [ ] #10 Given couple_top20 holding u-a at 0 and u-b at 1 When the partner calls reorderPortalList with [u-b 0, u-a 1] Then the answer is 200 with u-b at 0 and u-a at 1, because the position uniqueness is checked at commit (BR-DM-26).
- [ ] #11 Given rekord-contract with start_pref in SongEntry (rekord-contract/components/music.yaml:165, :209-214) and EntryInput (:312-316) When the contract pull request of this ticket is merged after its contract-push approval Then neither schema has a start_pref property, the pull request carries the label semver:major because it removes a property and raises info.version by the major part (UD-19.e, UD-20.b), and the SongEntry and EntryInput classes wedding-portal generates from it have no start_pref field (deviation UD-18.d).
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
