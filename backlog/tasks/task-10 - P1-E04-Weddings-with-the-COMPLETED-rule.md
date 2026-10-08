---
id: TASK-10
title: P1-E04 Weddings with the COMPLETED rule
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - epic
  - P1
milestone: m-1
dependencies:
  - TASK-9
references:
  - 'docs/rewrite/STATUS.md:261-269'
  - rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java
  - rekord-api/src/main/java/app/rekord/wedding/WeddingService.java
  - rekord-contract/paths/planner.yaml
  - docs/rewrite/analysis/11-planner-weddings.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 10400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to create, list, read, update and delete the weddings of my business so that each wedding has one record the couple portal and the DJ work from.

Goal: the wedding aggregate. Scope: the operations listWeddings, createWedding, getWedding, updateWedding and deleteWedding (deleteWedding is built test-first, UD-15.d); the COMPLETED rule of UD-11 (a venue and DJ and PHOTO slots each PENCILLED, CONFIRMED or NOT_NEEDED, checked on create, on update and on every change to a COMPLETED wedding, every other status move free), refused through UD-12; the form that re-sends every team slot as pencilled (RISK-21), the couple rename (RISK-31) and the listWeddings defaults (RISK-46). Out of scope: the off-contract DJ couple CRUD of the POC (UX-02) and the team-slot operations (P1-E05).

Plan item `P1-E04` (epic,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a wedding with a venue, its DJ slot CONFIRMED and its PHOTO slot OPEN When a planner calls updateWedding to set the status to COMPLETED Then the answer is 422 VALIDATION_FAILED naming the PHOTO slot with the errors item {field team.PHOTO, code INVALID_VALUE}, and the status is unchanged (deviation UD-11, UD-19.d1).
- [ ] #2 Given a wedding without a venue When a planner calls createWedding or updateWedding with the status COMPLETED Then the answer is 422 VALIDATION_FAILED naming the venue with the errors item {field venue, code INVALID_VALUE} (deviation UD-11, UD-19.d1; rekord-api answers 500).
- [ ] #3 Given a COMPLETED wedding When a planner calls updateWedding to set any other status Then the answer is 200 and the new status is stored (UD-11).
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
