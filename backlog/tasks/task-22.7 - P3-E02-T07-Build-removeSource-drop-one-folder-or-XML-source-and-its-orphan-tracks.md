---
id: TASK-22.7
title: >-
  P3-E02-T07 Build removeSource: drop one folder or XML source and its orphan
  tracks
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-22.6
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:104-109'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:282-311'
  - 'rekord-contract/paths/dj.yaml:220-245'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:126'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:522'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:121-130'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:288-311'
  - 'rekord-api/src/main/resources/db/migration/V8__library.sql:119-131'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-22
priority: high
type: feature
ordinal: 30207
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to remove a folder or an XML export from my library so that tracks I no longer own stop being matched.

Removing a source drops it and its claims and deletes every track no source claims any more; tracks another source still claims stay, and remembered choices survive so a decision rebinds when the folder comes back (BR-LIB-15). The orphan sweep runs over every track without a claim, across businesses, because tracks are global (BR-DM-31, BR-LIB-10). Sources are created by the scan and the XML import of P3-E03; the tests of this ticket insert them as fixture rows. Invalidating the match index is settled by P3-E04-T05.

- Builds: `removeSource`
- Covers: BR-LIB-15, BR-DM-31, PIN-15-0092
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E02-T07` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the DJ's active library with source S1 claiming tracks T1 and T2 and source S2 claiming T2 and T3 When the DJ calls removeSource on S1 Then the answer is 200 with a LibrarySummary whose sources list only S2, the tracks row T1 is gone, and T2 and T3 remain.
- [ ] #2 Given a preference of the library that names track T1 When the DJ removes S1 Then no tracks row of T1 remains and the preference still exists with its track id, because preferences carry no foreign key to tracks (LibraryRepository.java:288-311).
- [ ] #3 Given a tracks row with no claim left by a library of another business When the DJ removes S1 Then that row is deleted too, and tracks claimed by that business's sources remain (BR-DM-31).
- [ ] #4 Given an unknown source id When the DJ calls removeSource Then the answer is 404 NO_SOURCE with the message "That source is gone.".
- [ ] #5 Given a source of a library owned by another DJ When the DJ calls removeSource on it Then the answer is 404 NO_LIBRARY and the source still exists (BR-LIB-15).
- [ ] #6 Given the DJ's library L whose folder source S claims track T1 and no other source claims T1, and the DJ's library M holding a preference that names T1 When the DJ calls deleteLibrary on L Then no tracks row of T1 remains and the preference of M that names T1 still exists (BR-DM-31, PIN-15-0092; LibraryRepository.java:121-130, :306-311).
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
