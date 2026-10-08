---
id: TASK-22.2
title: >-
  P3-E02-T02 Build getLibrary: the caller's libraries and the active library's
  panel
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.1
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:69-80'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryMapper.java:39-89'
  - 'rekord-contract/components/library.yaml:82-172'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:107-115'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:50'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:41'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30202
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see my libraries, the one I work in, its track count, its file types and its sources so that I know what my matching runs against.

A library belongs to one business and normally one owner. A DJ sees only the libraries they own; a planner also sees the business's unowned libraries, whose owner account is gone, ordered by id (BR-LIB-01). Libraries are per DJ (UX-06). The call always succeeds: with nothing active it returns the list with an empty active section (BR-LIB-09). UD-14 lets an admin do everything a planner may, so an admin sees what a planner sees. The summary built here is the answer of every library operation of P3-E02.

- Builds: `getLibrary`
- Covers: BR-LIB-01, BR-LIB-09, UX-06
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: the LibrarySummary of rekord-api re-reads the active library, which activeLibraryId has already checked as visible (LibraryMapper.java:66, LibraryRepository.java:175-189), so no request reaches 404 NO_LIBRARY here; no criterion

Plan item `P3-E02-T02` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ who owns libraries with ids 7 and 3, another DJ of the same business who owns library 5, and an unowned library 9 When the DJ calls getLibrary Then the answer is 200 and libraries lists exactly 3 and 7, in that order (BR-LIB-01).
- [ ] #2 Given the same data When a member holding the PLANNER role and owning library 11 calls getLibrary Then libraries lists 9 and 11, in id order, and never library 3, 5 or 7.
- [ ] #3 Given the same data When a member holding only the ADMIN role and owning no library calls getLibrary Then libraries lists exactly library 9 (deviation UD-14.b3; rekord-api answers 403 FORBIDDEN, LibraryResource.java:41).
- [ ] #4 Given a DJ with 2 libraries and no active library When the DJ calls getLibrary Then the answer is 200 with active_library_id null, no active_library_name, track_count 0, by_ext empty and sources empty (BR-LIB-09).
- [ ] #5 Given a DJ whose active library holds 3 mp3 tracks and 1 flac track from 2 sources When the DJ calls getLibrary Then active_library_id and active_library_name name it, track_count is 4, by_ext is {"mp3": 3, "flac": 1}, and sources lists both with id, library_id, kind, label, added_at and their track counts, in source id order (LibraryMapper.java:39-89).
- [ ] #6 Given a DJ who owns library 7 named "Studio PC" holding 3 scanned tracks from one folder source, and library 3 with no tracks and no source When the DJ calls getLibrary Then the entry of library 7 in libraries holds id 7, name "Studio PC", owner_id the DJ's account id, created_at the instant library 7 was created, track_count 3 and source_count 1, and the entry of library 3 holds track_count 0 and source_count 0 (LibraryMapper.java:44-58).
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
