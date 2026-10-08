---
id: TASK-22.6
title: 'P3-E02-T06 Build selectLibrary: the active library per member and per business'
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.5
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:98-102'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:82-95'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:175-213'
  - 'rekord-contract/paths/dj.yaml:1-11'
  - 'rekord-contract/paths/dj.yaml:197-219'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:108'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:111'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30206
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to choose the library I work in so that matching, imports and exports run against that computer's tracks.

The active library is a settings row per (business, member). A stored value that is not a number, or names a library the member can no longer see, reads as no active library, with no fallback to another library (BR-LIB-05, BR-DM-32). Every library route answers 404 NO_LIBRARY, never 403, for a library outside the caller's set, so ids cannot be enumerated (BR-LIB-02, rekord-contract/paths/dj.yaml:4-6). Two concurrent first selections are settled by P3-E09-T02.

- Builds: `selectLibrary`
- Covers: BR-LIB-05, BR-LIB-02
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the unique-constraint race of this operation is settled by P3-E09-T02 (both 200, never 500)

Plan item `P3-E02-T06` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ owning libraries A and B and no settings row When the DJ calls selectLibrary on B Then the answer is 200 with a LibrarySummary whose active_library_id is B, and one settings row for the DJ and business holds B.
- [ ] #2 Given a DJ whose active library is B When the DJ calls selectLibrary on A Then active_library_id is A and the DJ still has one settings row.
- [ ] #3 Given a DJ and a planner of one business who each select a library When either calls getLibrary Then each sees their own selection, because the selection is stored per member.
- [ ] #4 Given a DJ whose settings row holds the value "abc" When the DJ calls getLibrary Then active_library_id is null and the active section is empty (BR-DM-32).
- [ ] #5 Given a DJ whose settings row names a library the DJ can no longer see When the DJ calls getLibrary Then active_library_id is null, and no other library of the DJ is chosen in its place (BR-LIB-05).
- [ ] #6 Given a library owned by another DJ of the same business, a library of another business and an unknown id When the DJ calls selectLibrary, renameLibrary and deleteLibrary with each Then every call answers 404 NO_LIBRARY and none answers 403 (BR-LIB-02).
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
