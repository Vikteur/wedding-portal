---
id: TASK-22.3
title: >-
  P3-E02-T03 Build createLibrary: name rules, per-owner uniqueness and
  first-library selection
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.2
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:73-83'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:99-165'
  - 'rekord-api/src/main/resources/db/migration/V8__library.sql:39-45'
  - 'rekord-contract/components/library.yaml:174-181'
  - 'rekord-contract/paths/dj.yaml:125-146'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:109-112'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30203
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to create a library for each computer I play from so that each one's tracks are matched on their own.

Names are stripped and limited to 120 characters, both refused with 400 EMPTY_NAME (BR-LIB-03); an empty or missing JSON name never reaches that check, because the contract's minLength 1 answers 422 first. Names are unique per owner, case-insensitively, and the database index is the authority behind the 409 pre-check (BR-LIB-04). A new library becomes the active one only when the caller has none (BR-LIB-06). The answer is 201 with the summary of P3-E02-T02.

- Builds: `createLibrary`
- Covers: BR-LIB-03, BR-LIB-04, BR-LIB-06, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: the LibrarySummary of rekord-api re-reads the active library, which activeLibraryId has already checked as visible (LibraryMapper.java:66, LibraryRepository.java:175-189), so no request reaches 404 NO_LIBRARY here; no criterion
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the loser of a name race that only the unique index catches answers 409 DUPLICATE_NAME with the message "You already have a library called that.", the answer of the name check (deviation UD-19.i5, RISK-26; rekord-api answers 500 UNKNOWN), asserted by P3-E09-T02, criterion 1

Plan item `P3-E02-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ with no library When the DJ calls createLibrary with name "  Studio PC  " Then the answer is 201 with a LibrarySummary whose libraries hold one library named "Studio PC" owned by the DJ, and active_library_id is its id (BR-LIB-06).
- [ ] #2 Given a DJ whose active library is A When the DJ calls createLibrary with name "Laptop" Then the answer is 201, the new library is listed, and active_library_id is still A.
- [ ] #3 Given a DJ When the DJ calls createLibrary with name "   " and with a name of 121 characters after stripping Then each answers 400 EMPTY_NAME, with the messages "A library needs a name." and "That name is too long.", and no library is created (BR-LIB-03).
- [ ] #4 Given a DJ who owns a library named "Studio PC" When the DJ calls createLibrary with name "studio pc" Then the answer is 409 DUPLICATE_NAME with the message "You already have a library called that." (BR-LIB-04).
- [ ] #5 Given another DJ of the same business who owns a library named "Studio PC" When the DJ calls createLibrary with name "Studio PC" Then the answer is 201, because names are unique per owner.
- [ ] #6 Given a DJ When the DJ calls createLibrary with the body {}, with the body abc, which is not JSON, and with the body {"name":5} Then each answers 422 VALIDATION_FAILED in the error envelope, the errors items are {field "name", code "REQUIRED"}, {field null, code "INVALID_FORMAT"} and {field "name", code "INVALID_FORMAT"} in that order of calls, and no library is created (UD-19.d1, UD-19.d3).
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
