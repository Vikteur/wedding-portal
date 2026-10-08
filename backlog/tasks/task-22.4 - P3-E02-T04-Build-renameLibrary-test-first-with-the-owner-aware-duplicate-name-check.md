---
id: TASK-22.4
title: >-
  P3-E02-T04 Build renameLibrary test-first with the owner-aware duplicate-name
  check
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - test-first
milestone: m-3
dependencies:
  - TASK-22.3
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:85-89'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:111-165'
  - 'rekord-api/src/main/resources/db/migration/V8__library.sql:39-45'
  - 'rekord-contract/paths/dj.yaml:147-173'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:521'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:435'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:499'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:508'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30204
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to rename a library so that its name says which computer it belongs to.

rekord-api has no test for this operation, so it is built test-first (UD-15.d). rekord-api's duplicate-name pre-check always looks at the caller's own libraries (LibraryRepository.java:148-165): a planner renaming an unowned library to the name of another unowned library gets a 500 from the unique index, and one renaming it to the name of one of their own libraries gets a false 409 (BR-DM-30, analysis 13 R3). wedding-portal checks the names of the renamed library's own owner scope, which is what the two unique indexes enforce. The empty-name answer of the contract is pinned for both name operations (PIN-13-0499).

- Builds: `renameLibrary`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-DM-30, PIN-13-0508, PIN-13-0499, PIN-15-0091, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the loser of a name race that only the unique index catches answers 409 DUPLICATE_NAME with the message "You already have a library called that.", the answer of the name check (deviation UD-19.i5, RISK-26; rekord-api answers 500 UNKNOWN), asserted by P3-E09-T02, criterion 2

Plan item `P3-E02-T04` (user-story,P3,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and a DJ owning libraries "A" and "B" When a characterization test calls renameLibrary on "A" with "Gig laptop", with "b", with "   " and for a library of another DJ Then it records 200 with the summary, 409 DUPLICATE_NAME, 400 EMPTY_NAME and 404 NO_LIBRARY, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a DJ owning library "A" When the DJ calls renameLibrary on it with "  Gig laptop " Then the answer is 200 with a LibrarySummary in which that library is named "Gig laptop".
- [ ] #3 Given a DJ owning library "A" When the DJ renames it to "a" Then the answer is 200, because the clash check leaves out the renamed library itself.
- [ ] #4 Given two unowned libraries "Old" and "Older" of one business When a planner calls renameLibrary to name "Older" as "old" Then the answer is 409 DUPLICATE_NAME (fixed defect BR-DM-30, PIN-13-0508, PIN-15-0091; rekord-api answers 500).
- [ ] #5 Given a planner who owns library "Office" and an unowned library "Old" When the planner renames "Old" to "Office" Then the answer is 200 (fixed defect BR-DM-30; rekord-api answers a false 409 DUPLICATE_NAME).
- [ ] #6 Given a DJ When the DJ calls renameLibrary with "   " or a name of 121 characters after stripping Then each answers 400 EMPTY_NAME, and the name is unchanged.
- [ ] #7 Given a DJ When the DJ calls createLibrary or renameLibrary with the body {"name":""} Then each answers 422 VALIDATION_FAILED in the error envelope (PIN-13-0499).
- [ ] #8 Given a library owned by another DJ and an unknown library id When the DJ calls renameLibrary with each Then each answers 404 NO_LIBRARY with the message "That library does not exist.".
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
