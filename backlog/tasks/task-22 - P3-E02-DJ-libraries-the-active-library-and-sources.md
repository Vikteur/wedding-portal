---
id: TASK-22
title: 'P3-E02 DJ libraries, the active library and sources'
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-21
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:72-189'
  - rekord-api/src/main/java/app/rekord/library/LibraryResource.java
  - rekord-api/src/main/java/app/rekord/library/LibraryMapper.java
  - rekord-contract/paths/dj.yaml
  - docs/rewrite/analysis/13-dj-library-ingestion.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to keep my own music libraries and choose the active one so that matching and exports use the collection I play from.

Goal: the library aggregate. Scope: the operations getLibrary, createLibrary, renameLibrary (test-first), deleteLibrary (test-first), selectLibrary and removeSource; ownership and visibility with 404 NO_LIBRARY for a library outside the caller's set (BR-LIB-01, BR-LIB-02); names, the active library and its selection rules (BR-LIB-03 to BR-LIB-07); the delete cascade (BR-LIB-08); per-DJ libraries (UX-06); the rename pre-check that answers 409 or 500 wrongly for planners (RISK-48). Out of scope: scanning and importing tracks (P3-E03).

Plan item `P3-E02` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ with no library When the DJ calls createLibrary with a new name Then the library exists, is owned by the DJ and is the DJ's active library.
- [ ] #2 Given a library owned by another DJ When the DJ calls renameLibrary, deleteLibrary or selectLibrary with its id Then each answers 404 NO_LIBRARY.
- [ ] #3 Given two unowned libraries of one business When a planner calls renameLibrary to give the first one the name of the second, with no other call running Then the answer is 409 DUPLICATE_NAME (fixed defect BR-DM-30, PIN-13-0508; rekord-api answers 500; the races of RISK-48 are P3-E09-T02).
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
