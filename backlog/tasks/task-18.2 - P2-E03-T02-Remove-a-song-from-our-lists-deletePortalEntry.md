---
id: TASK-18.2
title: P2-E03-T02 Remove a song from our lists (deletePortalEntry)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-18.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:123-135'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:96-111'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:202-215'
  - 'docs/rewrite/analysis/12-couple-portal.md:94-95'
  - 'docs/rewrite/analysis/12-couple-portal.md:520'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-18
priority: high
type: feature
ordinal: 20302
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to remove a song from one of our lists so that the DJ no longer sees it.

Removing an entry. Oracle: PortalResource.java:123-135 (couple only) and SongService.remove (SongService.java:96-111), which is idempotent, finds the row by wedding and uid whatever its list, and recounts the list. Nothing in rekord-api marks a list SUBMITTED or LOCKED, a list never moves back to WAITING, and the never list counts toward progress (RISK-45, slice 12 R13). UD-18.c: lists are never SUBMITTED or LOCKED, there is no submit or lock step, the list state field stays in the contract because the frontends are unchanged, and the never list does not count toward progress; phase 1 seeds the list kinds (P1-E04-T01) and builds the progress count of getWeddingSongLists (P1-E06-T06), where the same decision applies. deletePortalEntry has rekord-api tests, so it is not built test-first. UD-18.a and UD-19.l1: every deletePortalEntry call of a FRIENDS session stays refused, whatever uid it names, as by rekord-api's couple-only check, but with 403 FORBIDDEN "Friends can add songs but cannot change or remove them." instead of rekord-api's 403 FORBIDDEN "That part belongs to the couple.".

- Builds: `deletePortalEntry`
- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-21, RISK-45, UD-18.a, UD-18.c1

Plan item `P2-E03-T02` (user-story,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and couple_top20 holding u-1 and u-2 When the partner calls deletePortalEntry for u-1 Then the answer is 200 with entries only (no entry field), couple_top20 holds u-2 alone and its song_count is 1.
- [ ] #2 Given the same session When the partner calls deletePortalEntry for the unknown uid u-9 Then the answer is 200 and every list and song_count is unchanged.
- [ ] #3 Given f-1 saved on friends_top20 by a friend When the partner calls deletePortalEntry for f-1 Then the answer is 200 and f-1 is gone.
- [ ] #4 Given opening_dance IN_PROGRESS with one entry When the partner deletes that entry Then the list has song_count 0 and stays IN_PROGRESS, no portal call sets SUBMITTED or LOCKED, and getWeddingSongLists shows progress lists_total 5 with never left out (deviation UD-18.c; rekord-api counts never, giving lists_total 6).
- [ ] #5 Given a FRIENDS session of the same wedding and f-1 saved on friends_top20 through the friends' link When the friend calls deletePortalEntry for f-1 and for the unknown uid u-9 Then both answers are 403 FORBIDDEN "Friends can add songs but cannot change or remove them." and f-1 stays (deviation UD-18.a/UD-19.l1; rekord-api answers 403 FORBIDDEN "That part belongs to the couple.").
- [ ] #6 Given the same wedding When deletePortalEntry is called without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED.
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
