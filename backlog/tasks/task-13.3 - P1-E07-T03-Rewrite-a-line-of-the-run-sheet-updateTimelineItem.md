---
id: TASK-13.3
title: P1-E07-T03 Rewrite a line of the run sheet (updateTimelineItem)
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-13.2
references:
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:89-97'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:209-216'
  - 'rekord-api/src/main/java/app/rekord/planning/PlanningResource.java:226-237'
  - 'rekord-contract/components/planner.yaml:645-680'
  - 'rekord-contract/paths/planner.yaml:458-483'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-13
priority: high
type: feature
ordinal: 10703
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to change a line of the run sheet so that the day's plan follows every change the couple and the suppliers agree on.

Builds the operation updateTimelineItem. Oracle: PlanningResource.updateTimelineItem (PlanningResource.java:89-97) and requireItem (PlanningResource.java:209-216). The operation needs the PLANNER role. The line is found by id, else 404 NO_WEDDING "There is no such line."; its wedding is then found with the visibility rule of BR-PL-03, else 404 NO_WEDDING "There is no such wedding.", so a line of another business or of a deleted wedding answers that. Every field of the line is replaced by the body, a field left out included, and updated_at is set; the answer is 200 with the one TimelineItem, owner_label resolved as in P1-E07-T02 (BR-PL-39). Field rules of a line, shared by addTimelineItem and updateTimelineItem (PlanningResource.apply, PlanningResource.java:226-237): at_time is read with java.time.LocalTime.parse, so 20:30 and 20:30:00 are accepted, and is written back as LocalTime.toString writes it (20:30); what, duration_min, owner_team_id, owner_person_id and owner_role are taken from the body as sent, null included; a missing visibility becomes ALL; a missing day_offset becomes 1 when at_time is before 05:00 and 0 otherwise (BR-PL-35); sort_order is never set and stays 0 (BR-PL-39). Deviations under UD-12, each a 422 VALIDATION_FAILED whose message names every violation and whose errors list has one item per violation, where rekord-api answers 500: an at_time that LocalTime.parse rejects (field at_time), a day_offset other than 0 or 1 (field day_offset), and a body naming both owner_team_id and owner_person_id (field owner_person_id) (BR-PL-35, BR-PL-38, BR-PL-39, PIN-15-0090). A missing at_time and a missing or empty what are refused with 422 by the contract's validation, as in rekord-api. A body that breaks a contract rule and one of the rules above together gets a 422 that lists only the contract violations (UD-19.d2). The check that an owner id names a team slot or a person of the line's own wedding is P1-E09-T04. rekord-api has no test for this operation, so the ticket is test-first (UD-15.d).

- Builds: `updateTimelineItem`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E07-T03` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for updateTimelineItem When a characterization test calls updateTimelineItem against rekord-api with a full body, a body without the optional fields, an at_time of 00:30 without day_offset, an unknown line id and a line of a deleted wedding Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 with a line at_time 20:30, what "Opening dance", duration_min 15, owner_role DJ and visibility TEAM When a planner calls updateTimelineItem with only at_time 21:00 and what "Opening dance, floor opens after" Then the answer is 200 with at_time 21:00, day_offset 0, duration_min null, owner_role null, owner_label null and visibility ALL, and getTimeline shows the same values (BR-PL-39).
- [ ] #3 Given that line When a planner calls updateTimelineItem with at_time 00:30 and no day_offset, then with at_time 00:30 and day_offset 0 Then the first answer has day_offset 1 and the second has day_offset 0 (BR-PL-35).
- [ ] #4 Given that line and the PARTNER Emma of the wedding When a planner calls updateTimelineItem with at_time 20:30, what "Opening dance" and Emma's id as owner_person_id Then the answer has owner_person_id Emma's id and owner_label "Emma" (BR-PL-37).
- [ ] #5 Given that line When a planner calls updateTimelineItem with at_time "25:00" and what "Opening dance", then with at_time 20:30, what "Opening dance" and day_offset 2, then with at_time 20:30, what "Opening dance" and both owner ids of the wedding Then the answers are 422 VALIDATION_FAILED with the errors item {field at_time, code INVALID_VALUE}, {field day_offset, code INVALID_VALUE} and {field owner_person_id, code INVALID_VALUE} in turn, and the line is unchanged (deviation UD-12; rekord-api answers 500).
- [ ] #6 Given an unknown line id When a planner calls updateTimelineItem Then the answer is 404 NO_WEDDING "There is no such line.", and for a line of a wedding of another business and a line of a wedding with deleted_at set the answer is 404 NO_WEDDING "There is no such wedding." with the line unchanged.
- [ ] #7 Given that line and a member holding only DJ named on the wedding's DJ slot When the DJ calls updateTimelineItem Then the answer is 403 and the line is unchanged.
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
