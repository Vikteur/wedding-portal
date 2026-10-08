---
id: TASK-13.2
title: >-
  P1-E07-T02 Read the run sheet and add a line to it (getTimeline,
  addTimelineItem)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-13.1
  - TASK-10.2
  - TASK-11.1
  - TASK-11.5
references:
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:36-87'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:204-207'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:226-237'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:258-305'
  - 'rekord-api/src/main/resources/db/migration/V7__planning.sql:3-38'
  - 'rekord-contract/components/planner.yaml:592-689'
  - 'rekord-contract/components/common.yaml:130-134'
  - 'rekord-contract/paths/planner.yaml:410-458'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:162-224'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-13
priority: high
type: feature
ordinal: 10702
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to add lines to a wedding's run sheet and read them in the order of the day so that the small hours after midnight come last and my private lines stay mine.

Builds the operations getTimeline and addTimelineItem. Oracle: PlanningResource.getTimeline and addTimelineItem (PlanningResource.java:56-87), requireWedding (PlanningResource.java:204-207), the mapper timelineItem and ownerLabel (PlanningResource.java:264-305). getTimeline is open to every signed-in member: the wedding is found with the visibility rule of BR-PL-03, else 404 NO_WEDDING "There is no such wedding."; a caller without the PLANNER role sees only the lines with visibility ALL or TEAM (BR-PL-36); the lines are ordered by day_offset, at_time and sort_order (BR-PL-35). addTimelineItem needs the PLANNER role, finds the wedding the same way, stores the line with the wedding's org_id and answers 201 with the whole list as getTimeline returns it to the caller (BR-PL-39). owner_label is resolved on every read and never stored (BR-PL-37): for an owner person their given name; for an owner team slot its person_name, else the name of its vendor, else null; for a line with only an owner_role, or no owner, null. The vendor contact is never consulted. Resolving these names only within the business is P1-E09-T04 (RISK-10). Field rules of a line, shared by addTimelineItem and updateTimelineItem (PlanningResource.apply, PlanningResource.java:226-237): at_time is read with java.time.LocalTime.parse, so 20:30 and 20:30:00 are accepted, and is written back as LocalTime.toString writes it (20:30); what, duration_min, owner_team_id, owner_person_id and owner_role are taken from the body as sent, null included; a missing visibility becomes ALL; a missing day_offset becomes 1 when at_time is before 05:00 and 0 otherwise (BR-PL-35); sort_order is never set and stays 0 (BR-PL-39). Deviations under UD-12, each a 422 VALIDATION_FAILED whose message names every violation and whose errors list has one item per violation, where rekord-api answers 500: an at_time that LocalTime.parse rejects (field at_time), a day_offset other than 0 or 1 (field day_offset), and a body naming both owner_team_id and owner_person_id (field owner_person_id) (BR-PL-35, BR-PL-38, BR-PL-39, PIN-15-0090). A missing at_time and a missing or empty what are refused with 422 by the contract's validation, as in rekord-api. A body that breaks a contract rule and one of the rules above together gets a 422 that lists only the contract violations (UD-19.d2). The check that an owner id names a team slot or a person of the line's own wedding is P1-E09-T04.

- Builds: `getTimeline`, `addTimelineItem`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-35, BR-PL-36, BR-PL-37
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E07-T02` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 When a planner calls addTimelineItem with at_time 01:00 and what "Last song, lights up", then with at_time 08:00 and what "Photographer arrives" Then each answer is 201 with the whole list, and getTimeline answers 200 with items "Photographer arrives" (day_offset 0) then "Last song, lights up" (day_offset 1) (BR-PL-35).
- [ ] #2 Given that wedding with the line at_time 08:00 "Photographer arrives" When a planner calls addTimelineItem with at_time 01:30, day_offset 0 and what "Doors open" Then the line is stored with day_offset 0 and getTimeline lists it before "Photographer arrives" (BR-PL-35).
- [ ] #3 Given that wedding When a planner calls addTimelineItem with only at_time 20:30 and what "Opening dance" Then the new item has at_time 20:30, day_offset 0, duration_min null, owner_team_id, owner_person_id, owner_role and owner_label null, and visibility ALL, and the row holds sort_order 0 (BR-PL-39).
- [ ] #4 Given that wedding When a planner calls addTimelineItem with at_time 20:30, what "Opening dance" and owner_role DJ Then the item has owner_role DJ and owner_label null (BR-PL-37).
- [ ] #5 Given that wedding with the PARTNER Emma, a CATERING slot with person_name "Ray", a PHOTO slot with the vendor "Studio Licht" and its contact "Mila" and no person_name, and a VENUE slot with neither When a planner adds one line owned by each of the four, by owner_person_id or owner_team_id, and calls getTimeline Then the owner_label values are "Emma", "Ray", "Studio Licht" and null (BR-PL-37).
- [ ] #6 Given that wedding with one line each of visibility ALL, TEAM and PLANNER_ONLY, and a member holding only DJ named on its DJ slot as CONFIRMED When the planner and the DJ each call getTimeline Then the planner gets the 3 lines and the DJ gets the ALL and TEAM lines only (BR-PL-36).
- [ ] #7 Given that wedding When a planner calls addTimelineItem with at_time "25:00" and what "Opening dance", then with at_time 20:30, what "Opening dance" and day_offset 2, then with at_time 20:30, what "Opening dance" and both an owner_team_id and an owner_person_id of the wedding Then the answers are 422 VALIDATION_FAILED with the errors item {field at_time, code INVALID_VALUE}, {field day_offset, code INVALID_VALUE} and {field owner_person_id, code INVALID_VALUE} in turn, and getTimeline lists no new line (deviation UD-12; rekord-api answers 500, BR-PL-38, BR-PL-39).
- [ ] #8 Given that wedding When a planner calls addTimelineItem with at_time "half past eight", what "Opening dance" and day_offset 3 Then the answer is one 422 VALIDATION_FAILED whose errors list holds {field at_time, code INVALID_VALUE} and {field day_offset, code INVALID_VALUE} (UD-12, UD-19.d1).
- [ ] #9 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls getTimeline and addTimelineItem with each Then each answer is 404 NO_WEDDING "There is no such wedding." and no line is stored.
- [ ] #10 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and a member holding only DJ named on no slot of it When the DJ calls getTimeline Then the answer is 404 NO_WEDDING "There is no such wedding.".
- [ ] #11 Given that wedding and a member holding only DJ named on its DJ slot as CONFIRMED When the DJ calls addTimelineItem with at_time 20:30 and what "Opening dance" Then the answer is 403 and no line is stored.
- [ ] #12 Given that wedding When a planner calls addTimelineItem with at_time 20:30, what "" and day_offset 3 Then the answer is one 422 VALIDATION_FAILED whose errors list holds only {field what, code TOO_SHORT}, and getTimeline lists no new line (UD-19.d2).
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
