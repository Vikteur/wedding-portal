---
id: TASK-13
title: P1-E07 Wedding timeline
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-10
references:
  - rekord-api/src/main/java/app/rekord/planning/PlanningResource.java
  - rekord-api/src/main/java/app/rekord/domain/TimelineItem.java
  - rekord-contract/paths/planner.yaml
  - docs/rewrite/analysis/11-planner-weddings.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10700
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to keep the timeline of each wedding day so that the couple sees the order of the day in the portal.

Goal: the timeline of a wedding. Scope: the operations getTimeline, addTimelineItem, updateTimelineItem and deleteTimelineItem; updateTimelineItem and deleteTimelineItem have no rekord-api test and are built test-first (UD-15.d); owner labels resolved only within the business (RISK-10). Out of scope: the couple's read-only view (P2-E02).

Plan item `P1-E07` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding of the caller's business When a planner calls addTimelineItem, updateTimelineItem and deleteTimelineItem in turn Then getTimeline returns the items as they stand after each call.
- [ ] #2 Given updateTimelineItem and deleteTimelineItem, which rekord-api has no test for When their build tickets start Then a characterization test that pins the rekord-api answer is merged before the build (UD-15.d).
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
