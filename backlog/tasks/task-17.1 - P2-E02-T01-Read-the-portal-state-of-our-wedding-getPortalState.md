---
id: TASK-17.1
title: P2-E02-T01 Read the portal state of our wedding (getPortalState)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
  - stop-db-migration
milestone: m-2
dependencies:
  - TASK-16.2
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:68-72'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:286-312'
  - 'rekord-api/src/main/java/app/rekord/portal/SongMapper.java:76-114'
  - 'rekord-contract/components/common.yaml:172-188'
  - 'rekord-contract/components/music.yaml:465-511'
  - 'docs/rewrite/analysis/12-couple-portal.md:163'
  - 'docs/rewrite/analysis/12-couple-portal.md:197'
  - 'rekord-api/src/main/resources/db/migration/V10__briefing_text.sql:17-22'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:59-102'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-17
priority: high
type: feature
ordinal: 20201
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to open our portal and see our names, date, lists and limits so that we know what is still to fill in.

The read model the couple app and the friends' page start from. Oracle: PortalResource.state (PortalResource.java:286-312) with SongMapper.caps and entryMap (SongMapper.java:76-114). Every /guest/{token} route takes the path token as GuestToken with minLength 20 (rekord-contract/components/common.yaml:172-188), checked before the gate. getPortalState has rekord-api tests, so it is not built test-first; P1 and P3 of slice 12 section 10 are its pinned cases. This ticket's Flyway migration adds weddings.briefing_text text NULL (oracle V10__briefing_text.sql:17-22) and creates the song_entries table of V6__music.sql:59-102 with ux_song_entries_uid over wedding and uid (:92), the deferrable ux_song_entries_position over list and position (:97-100) and ix_song_entries_list (:102), without the start_pref column, which wedding-portal does not store (UD-18.d).

- Builds: `getPortalState`
- STOP (human approval in the pull request): auth-access, db-migration
- Covers: PIN-12-0163, PIN-12-0197

Plan item `P2-E02-T01` (user-story,P2,stop-auth-access,stop-db-migration) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027, briefing_text "First dance at 21:00", 2 entries on couple_top20, 1 on friends_top20 and 1 on never When the partner calls getPortalState Then the answer is 200 with scope COUPLE, names "Emma & Julian", wedding_date 2027-06-12, briefing_text "First dance at 21:00", the never entry in blocklist, the 3 other entries under entries and friends_link "/g/" followed by the live FRIENDS token.
- [ ] #2 Given a FRIENDS session of the same wedding When the friend calls getPortalState Then the answer is 200 with scope FRIENDS, names, wedding_date, caps, search_available and entries holding only friends_top20, and the body has no briefing_text, no blocklist and no friends_link.
- [ ] #3 Given any portal session of the same wedding When getPortalState is called Then caps is {opening_dance: 1, second_third: 2, couple_top20: 20, friends_top20: 20, must_plays: 5, playlist_links: null} and has no never key.
- [ ] #4 Given a COUPLE session where couple_top20 holds entries at positions 2 and 0, must_plays holds one entry saved without an artist, and opening_dance is empty When getPortalState is called Then entries lists couple_top20 before must_plays (the seeded order opening_dance, second_third, couple_top20, friends_top20, must_plays, playlist_links), each kind's entries ordered by position, has no opening_dance key, and the must_plays entry has artist "".
- [ ] #5 Given a COUPLE session of a wedding whose FRIENDS portal the planner revoked When getPortalState is called Then the answer is 200 with no friends_link.
- [ ] #6 Given a COUPLE session When getPortalState is called once with both search credentials configured and once with the client secret blank Then search_available is true and false.
- [ ] #7 Given the same wedding When getPortalState is called with its COUPLE token and no cookie, with another wedding's COUPLE token and this wedding's COUPLE cookie, and with a 10-character token and a valid cookie Then the answers are 401 NOT_SIGNED_IN "Sign in to continue." (slice 12 P1), 401 BAD_LINK "That link and code do not match. Check the code your DJ gave you.", and 422 VALIDATION_FAILED (slice 12 P3).
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
