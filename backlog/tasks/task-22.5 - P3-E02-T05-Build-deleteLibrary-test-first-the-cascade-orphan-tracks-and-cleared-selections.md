---
id: TASK-22.5
title: >-
  P3-E02-T05 Build deleteLibrary test-first: the cascade, orphan tracks and
  cleared selections
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - test-first
milestone: m-3
dependencies:
  - TASK-22.4
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:91-96'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:121-130'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:224-233'
  - 'rekord-api/src/main/resources/db/migration/V8__library.sql:47-133'
  - 'rekord-contract/paths/dj.yaml:174-196'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:114'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:523'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:175-189'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30205
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to delete a library I no longer use so that it stops showing up and its tracks no longer count.

rekord-api has no test for this operation, so it is built test-first (UD-15.d). Deleting a library cascades to its sources, claims, imported playlists and remembered choices, deletes the tracks no source claims any more, and clears the active-library selection of every member of the business that pointed at it; files on disk are never touched (BR-LIB-08, BR-DM-32). Dropping the library's cached match index is settled by P3-E04-T05.

- Builds: `deleteLibrary`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-LIB-08, BR-DM-32, PIN-15-0093
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E02-T05` (user-story,P3,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and a DJ owning library L with one source When a characterization test calls deleteLibrary on L and on a library of another DJ Then it records 200 with the summary without L and 404 NO_LIBRARY, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a DJ owning library L with 2 sources claiming tracks T1 and T2, an imported playlist and a preference When the DJ calls deleteLibrary on L Then the answer is 200 with a LibrarySummary that no longer lists L, and no source, claim, imported playlist or preference of L exists.
- [ ] #3 Given track T1 claimed only by a source of L and track T3 claimed by a source of L and by a source of library M When the DJ deletes L Then the tracks row T1 is gone and T3 remains.
- [ ] #4 Given L is the active library of the DJ and of a planner of the same business, and a settings row of another business holds the same library id value When the DJ deletes L Then the two settings rows of the DJ's business are gone and the other business's row remains (BR-DM-32).
- [ ] #5 Given the files a scan of L read When the DJ deletes L Then every file is still on disk unchanged.
- [ ] #6 Given a library owned by another DJ and an unknown library id When the DJ calls deleteLibrary with each Then each answers 404 NO_LIBRARY and nothing is deleted.
- [ ] #7 Given a DJ who owns only library L and has selected it with selectLibrary When the DJ calls deleteLibrary on L and then getLibrary Then getLibrary answers 200 with active_library_id null, active_library_name null, track_count 0, by_ext {}, sources [] and libraries [] (BR-DM-32, PIN-15-0093; LibraryRepository.java:121-130, :224-233).
- [ ] #8 Given a DJ who owns libraries L and M and whose active_library_id settings row holds the non-numeric value "abc" When the DJ calls deleteLibrary on L Then the answer is 200 with active_library_id null and libraries listing only M, and the settings row still holds "abc", because only rows whose value is L's id are deleted (PIN-15-0093; LibraryRepository.java:175-189, :224-233).
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
