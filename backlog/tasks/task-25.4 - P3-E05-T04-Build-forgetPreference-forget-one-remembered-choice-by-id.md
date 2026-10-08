---
id: TASK-25.4
title: 'P3-E05-T04 Build forgetPreference: forget one remembered choice by id'
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-25.2
  - TASK-25.3
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/PreferenceService.java:116-130'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:198-203'
  - 'rekord-contract/paths/dj.yaml:554-577'
  - >-
    rekord-api/src/test/java/app/rekord/matcher/MatchingResourceTest.java:217-243
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:143'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-25
priority: high
type: feature
ordinal: 30504
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to forget one version choice so that the matcher stops forcing that file for that song.

Forget deletes the choice with the given id in the active library only and answers the remaining list; when no row of the active library has that id the answer is 404 NO_PREFERENCE (BR-MX-23, PreferenceService.java:116-130). A choice of another library, the DJ's own or another DJ's, is outside the active library and answers the same 404.

- Builds: `forgetPreference`
- Covers: BR-MX-23
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN, such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `NO_LIBRARY` without a criterion here: requireActive answers 404 NO_LIBRARY only when the selected library is deleted by a concurrent request between activeLibraryId, which keeps a selection only while the library is visible (LibraryRepository.java:174-188), and the re-read in require (LibraryRepository.java:215-222); no criterion

Plan item `P3-E05-T04` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an active library L with remembered choices X and Y When the DJ calls forgetPreference with the id of X Then the answer is 200 with preferences holding only Y (MatchingResourceTest.java:217-243).
- [ ] #2 Given that X was forgotten When the DJ calls forgetPreference with the id of X again Then the answer is 404 NO_PREFERENCE with the message "No remembered choice with that id.".
- [ ] #3 Given the DJ's library M, not the active one, and another DJ's library, each holding a choice with id "4cad791cad0c0451", and L holding none When the DJ calls forgetPreference with that id Then the answer is 404 NO_PREFERENCE and both rows remain.
- [ ] #4 Given a DJ with no active library When the DJ calls forgetPreference with any id Then the answer is 400 NO_LIBRARY_SELECTED with the message "Select a library first.".
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
