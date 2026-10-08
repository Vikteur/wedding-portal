---
id: TASK-21
title: 'P3-E01 DJ weddings, the DJ view of song lists and the change feed'
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-20
  - TASK-11
references:
  - 'docs/rewrite/STATUS.md:298-302'
  - rekord-api/src/main/java/app/rekord/portal/CouplesResource.java
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:45'
  - rekord-contract/paths/dj.yaml
  - docs/rewrite/analysis/14-dj-matching-exports.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see the weddings I am booked for, their song lists and what the couple changed so that I prepare each night from the couple's latest wishes.

Goal: the DJ's window on the weddings. Scope: the operations listDjWeddings, getDjWedding (test-first, UD-15.d), getDjSongLists and getWeddingChanges; visibility by team assignment in state PENCILLED or CONFIRMED with 404 NO_WEDDING for any other id (BR-MX-42); an admin calling every DJ operation on every wedding of the business without an assignment (UD-14.b3, a STOP item); the change feed capped at 500 rows (UX-04). getDjWedding leaves out the couple's portal link, so neither the portal token nor the access code reaches the DJ, while the people keep their contact fields (deviation UD-19.m1, RISK-04). Song lists have no submit or lock step: every entry the couple or the friends save is in getDjSongLists the next time the DJ loads it, with no live push (deviation UD-18.c). Out of scope: the planner side of the team (P1-E05).

Plan item `P3-E01` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ who fills the DJ slot of one wedding as PENCILLED and of no other wedding When the DJ calls listDjWeddings Then exactly that wedding is listed, and getDjWedding with any other wedding id answers 404 NO_WEDDING.
- [ ] #2 Given an admin without a team-slot assignment When the admin calls getDjWedding, getDjSongLists and getWeddingChanges for a wedding of the business Then each answers 200 (deviation UD-14.b3).
- [ ] #3 Given a visible wedding with 600 change rows When the DJ calls getWeddingChanges with limit=1000 Then exactly 500 rows are returned, newest first, and limit=0 answers 422 VALIDATION_FAILED.
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
