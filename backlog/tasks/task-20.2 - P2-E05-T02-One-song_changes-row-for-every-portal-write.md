---
id: TASK-20.2
title: P2-E05-T02 One song_changes row for every portal write
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P2
milestone: m-2
dependencies:
  - TASK-17.2
  - TASK-18.2
  - TASK-18.3
  - TASK-19.2
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:74-188'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:218-231'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:133-148'
  - 'docs/rewrite/analysis/12-couple-portal.md:96'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-20
priority: high
type: feature
ordinal: 20502
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want every change the couple or their friends make to their lists recorded with who made it so that I see what changed since I last looked.

The change record the DJ's feed reads in P3-E01. Oracle: PortalResource writes one song_changes row through SongService (SongService.java:218-231) inside the caller's transaction for every portal write, with the source, action, list kind, uid and an English summary. The actions are added, removed, reordered and details only. Planner link actions write their own dj rows (UD-16.UX-03, P1-E06-T03 to P1-E06-T05). The rows are never pruned. Slice 12 section 10 question 4 asks whether a re-save should be recorded as updated and whether an unchanged details edit should be skipped; no user decision changes the oracle, so it is built. The song_changes table and its writer come from P1-E06-T01.

- Covers: BR-CP-23, UD-19.l2

Plan item `P2-E05-T02` (user-story,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session and a FRIENDS session of a wedding When the couple saves u-1 "Dancing Queen" on couple_top20, re-saves it, and a friend saves f-1 "Waterloo" on friends_top20 Then 3 song_changes rows exist: couple, added, couple_top20, u-1, "The couple added Dancing Queen" twice, and friend, added, friends_top20, f-1, "A friend added Waterloo".
- [ ] #2 Given u-1 on couple_top20 When the couple calls deletePortalEntry for u-1 and for the unknown uid u-9 Then 2 rows exist: couple, removed, no kind, u-1, "Removed a song" and couple, removed, no kind, u-9, "Removed a song".
- [ ] #3 Given must_plays with 2 entries When the couple reorders must_plays Then one row exists: couple, reordered, must_plays, no uid, "Reordered must_plays".
- [ ] #4 Given an empty never list When the couple bans b-1 "Macarena" and allows b-1 again Then 2 rows exist: couple, added, never, b-1, "Banned Macarena" and couple, removed, never, b-1, "Allowed a song again".
- [ ] #5 Given a COUPLE session When the couple calls updatePortalCouple with new names, and again with an empty body Then 2 rows exist, each couple, details, no kind, no uid, "Updated their details".
- [ ] #6 Given a FRIENDS session and its entry f-1 on friends_top20 When the friend calls putPortalEntry for must_plays, which answers 403, repeats the identical save of f-1, which answers 200 (UD-19.l2), and calls putPortalEntry for f-1 with a new title, which answers 403, and the couple calls putPortalEntry with an empty title, which answers 400 Then no song_changes row is added.
- [ ] #7 Given a COUPLE session and a test hook that makes the putPortalEntry transaction for the new uid u-c fail after its song_changes row was flushed When the partner calls putPortalEntry for u-c Then the answer is 500 UNKNOWN, and neither u-c nor a song_changes row for u-c is stored.
- [ ] #8 Given a wedding whose planner calls rotateWeddingPortal, revokeWeddingPortal and reissueAccessCode When the song_changes rows are read Then the only rows added are the source_kind dj, action details rows that P1-E06-T03 to P1-E06-T05 state, and no row has source_kind couple or friend (deviation UD-16.UX-03; rekord-api adds no row for rotate, revoke or reissue, digest-P2.md:55).
- [ ] #9 Given a song_changes row with at 2026-01-01 When the suite runs on 2027-06-01 after any portal write Then that row is still present.
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
