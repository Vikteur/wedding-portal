---
id: TASK-17.3
title: >-
  P2-E02-T03 Show the couple the timeline items shared with everyone
  (getPortalTimeline)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - test-first
  - stop-auth-access
milestone: m-2
dependencies:
  - TASK-17.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:196-218'
  - 'docs/rewrite/analysis/12-couple-portal.md:101'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-17
priority: high
type: feature
ordinal: 20203
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to read the timeline our planner shared with us so that we know when each moment of the day happens.

The couple's read-only view of the planner's timeline. rekord-api has no test for it, so it is built test-first (UD-15.d). Oracle: PortalResource.java:196-218 returns the wedding's items with visibility ALL, ordered by day offset, time and sort order, couple only. The timeline itself is built by the planner in P1-E07.

- Builds: `getPortalTimeline`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access

Plan item `P2-E02-T03` (user-story,P2,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a wedding with 5 timeline items, of which 3 have visibility ALL (day_offset 0 at 18:00, day_offset -1 at 20:00, day_offset 0 at 14:00), one TEAM and one PLANNER_ONLY When the partner calls getPortalTimeline, first against rekord-api as a characterization test and then against wedding-portal Then the answer is 200 with exactly the 3 ALL items in the order -1 20:00, 0 14:00, 0 18:00, each with id, day_offset, at_time as HH:mm, what and duration_min.
- [ ] #2 Given two ALL items both on day_offset 0 at 14:00 with sort_order 2 and 1 When the partner calls getPortalTimeline Then the item with sort_order 1 comes first.
- [ ] #3 Given wedding A with 2 ALL timeline items and wedding B with no timeline items When the partner of wedding B calls getPortalTimeline Then the answer is 200 with an empty items list.
- [ ] #4 Given a FRIENDS session of the same wedding When the friend calls getPortalTimeline Then the answer is 403 FORBIDDEN "That part belongs to the couple.".
- [ ] #5 Given the same wedding When getPortalTimeline is called without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED.
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
