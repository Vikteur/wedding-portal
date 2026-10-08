---
id: TASK-17
title: 'P2-E02 Couple details, portal state, timeline and tasks in the portal'
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - epic
  - P2
milestone: m-2
dependencies:
  - TASK-16
  - TASK-13
  - TASK-14
references:
  - rekord-api/src/main/java/app/rekord/portal/PortalResource.java
  - rekord-contract/paths/portal.yaml
  - docs/rewrite/analysis/12-couple-portal.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 20200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to see our wedding's state, timeline and tasks and set the names our portal shows and our briefing so that we know where the planning stands and our DJ reads our briefing.

Goal: what the couple reads and edits about the wedding. Scope: the operations getPortalState, updatePortalCouple, getPortalTimeline and getPortalTasks; updatePortalCouple, getPortalTimeline and getPortalTasks have no rekord-api test and are built test-first (UD-15.d); a wedding_date the couple sends ignored without a refusal and removed from the CoupleUpdate schema of the contract (UD-20.c, replacing the 422 of UD-18.b), and the couple's names kept apart from the planner's, with the access code unchanged (UD-18.b, RISK-30); the couple's own names shown to the couple and to their friends alike, and kept when the planner later renames the couple (UD-19.a, UD-19.l4). Out of scope: song wishes (P2-E03) and the never list (P2-E04).

Plan item `P2-E02` (epic,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an open portal session When a partner of the couple calls getPortalState, getPortalTimeline and getPortalTasks Then each answers 200 with the wedding's data in the contract's shape.
- [ ] #2 Given a COUPLE session of a wedding the planner created with couple_display_name "Emma & Julian" When a partner of the couple calls updatePortalCouple with names "Emma and Julian" Then getPortalState returns names "Emma and Julian" to the COUPLE session and to a FRIENDS session, and getWedding still returns couple_display_name "Emma & Julian" (deviation UD-18.b and UD-19.a).
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
