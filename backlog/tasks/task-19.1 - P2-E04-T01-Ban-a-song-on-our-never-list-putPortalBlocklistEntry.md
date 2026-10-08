---
id: TASK-19.1
title: P2-E04-T01 Ban a song on our never list (putPortalBlocklistEntry)
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
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:157-177'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:275-284'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:33-94'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:164-200'
  - 'docs/rewrite/analysis/12-couple-portal.md:103'
  - 'docs/rewrite/analysis/12-couple-portal.md:620-623'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-19
priority: high
type: feature
ordinal: 20401
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to put a song on our never list so that the DJ never plays it at our wedding.

The never list is a song-list kind flagged as a blocklist: stored as entries of kind never, uncapped, couple only, and returned apart from the other lists. Oracle: PortalResource.java:157-177 puts on never with no requested position and source couple through the shared SongService.put. Applying the never list at export is P3-E07. putPortalBlocklistEntry has rekord-api tests, so it is not built test-first.

- Builds: `putPortalBlocklistEntry`
- STOP (human approval in the pull request): auth-access
- Error code `LIST_FULL` without a criterion here: Unreachable here: the never list is uncapped, so SongService.freePosition appends instead of refusing (SongService.java:179-200; slice 12 disagreement 1).
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.
- Error code `UNKNOWN_ENTRY` without a criterion here: Reachable only when the wedding has no never-list row; createWedding seeds every list kind (P1-E04), and P2-E03-T01 pins the same answer of the shared put path.

Plan item `P2-E04-T01` (user-story,P2,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and an empty never list When the partner calls putPortalBlocklistEntry for the uid b-1 with title "Macarena" and artist "Los del Río" Then the answer is 200 with entry b-1 and blocklist [b-1], and getPortalState lists b-1 in blocklist and not under entries.
- [ ] #2 Given b-1 on the never list When the partner calls putPortalBlocklistEntry for b-1 with title "Macarena (Remix)" Then the answer is 200, the never list still holds one entry b-1 at its old position, and its title is "Macarena (Remix)".
- [ ] #3 Given the same session When the partner calls putPortalBlocklistEntry for a new uid with title "" and no free_text Then the answer is 400 EMPTY_TITLE "Pick a song or type one in first." and nothing is stored.
- [ ] #4 Given u-1 on couple_top20 When the partner calls putPortalBlocklistEntry for u-1 Then the answer is 409 UID_CONFLICT "That song is already on another list." and u-1 stays on couple_top20.
- [ ] #5 Given a never list holding 40 entries at positions 0 to 39 When the partner calls putPortalBlocklistEntry for a new uid Then the answer is 200 and the entry is at position 40, and the blocklist lists the entries by position.
- [ ] #6 Given a FRIENDS session of the same wedding When the friend calls putPortalBlocklistEntry Then the answer is 403 FORBIDDEN "That part belongs to the couple." and nothing is stored.
- [ ] #7 Given the same wedding When putPortalBlocklistEntry is called without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED.
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
