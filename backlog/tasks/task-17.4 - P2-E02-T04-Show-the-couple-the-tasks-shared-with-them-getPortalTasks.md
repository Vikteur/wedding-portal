---
id: TASK-17.4
title: P2-E02-T04 Show the couple the tasks shared with them (getPortalTasks)
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
  - TASK-17.3
references:
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:220-244'
  - 'docs/rewrite/analysis/12-couple-portal.md:101'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:196-218'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-17
priority: high
type: feature
ordinal: 20204
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to see the planning tasks our planner shared with us so that we know what is open before the day.

The couple's read-only view of the planning tasks. rekord-api has no test for it, so it is built test-first (UD-15.d). Oracle: PortalResource.java:220-244 returns the wedding's tasks with visible_to_couple that are not deleted, in any status, ordered by due date with no date last and then by title, couple only. The tasks are built by the planner in P1-E08.

- Builds: `getPortalTasks`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-CP-28

Plan item `P2-E02-T04` (user-story,P2,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session of a wedding with the tasks "Book cake tasting" (no due date), "Choose rings" (due 2027-05-01, DONE), "Pay venue" (due 2027-04-01, CANCELLED), "Order flowers" (not visible to the couple) and "Hire bus" (deleted) When the partner calls getPortalTasks, first against rekord-api as a characterization test and then against wedding-portal Then the answer is 200 with exactly "Pay venue", "Choose rings" and "Book cake tasting" in that order, each with id, wedding_id, title, detail, status, priority, due_date and visible_to_couple true.
- [ ] #2 Given two visible tasks "Send invites" and "Order cake" both due 2027-03-01 When the partner calls getPortalTasks Then "Order cake" comes before "Send invites".
- [ ] #3 Given visible tasks on another wedding and a visible task on no wedding When the partner calls getPortalTasks Then neither appears.
- [ ] #4 Given a FRIENDS session of the same wedding When the friend calls getPortalTasks Then the answer is 403 FORBIDDEN "That part belongs to the couple.".
- [ ] #5 Given the same wedding When getPortalTasks is called without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED.
- [ ] #6 Given a COUPLE session of a wedding with one timeline item of visibility ALL, one of visibility TEAM, one task visible to the couple and one not When the partner calls getPortalTimeline and getPortalTasks Then the answers hold only the ALL item and the visible task (BR-CP-28).
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
