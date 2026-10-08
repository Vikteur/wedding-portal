---
id: TASK-29.1
title: 'P3-E09-T01 Access matrix over every DJ operation, with the admin rule of UD-14'
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-28.5
  - TASK-27.4
  - TASK-26.6
  - TASK-21.4
  - TASK-6
  - TASK-15
references:
  - 'rekord-api/src/main/java/app/rekord/library/LibraryResource.java:41'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:52'
  - 'rekord-api/src/main/java/app/rekord/library/ExportsResource.java:46'
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:46-70'
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:138-141'
  - 'rekord-api/src/main/java/app/rekord/security/AppIdentity.java:81'
  - 'docs/rewrite/STATUS.md:296-306'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-29
priority: high
type: feature
ordinal: 30901
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want to call every DJ operation on every wedding of my business without a team slot so that I can stand in for any DJ, while sessions without a member role are kept out.

One table-driven test asserts who may call each operation of phase 3; the operation tickets refer to it for their NOT_SIGNED_IN and FORBIDDEN answers. rekord-api admits the library, scan, matching, preference and export operations for the roles DJ and PLANNER only (LibraryResource.java:41, MatchingResource.java:52, ExportsResource.java:46), and the DJ wedding operations for any session with a member account (CouplesResource.java:46, AppIdentity.requireUser). UD-14.b3 adds ADMIN: an admin may call every DJ operation on every wedding of the business without a team-slot assignment. The three per-couple operations of P3-E08 read the active library, so they take the library guard as exportPlaylist does, plus the wedding visibility of getDjWedding.

- STOP (human approval in the pull request): auth-access
- Covers: UD-14.b3, BR-ID-45, UD-19.i4

Plan item `P3-E09-T01` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given no session When a test calls each of listDjWeddings, getDjWedding, getDjSongLists, getWeddingChanges, getCoupleExportSummary, exportCoupleRekordboxXml and exportCoupleMissing Then each answers 401 NOT_SIGNED_IN from the route guard of P0-E06 and the operation does not run.
- [ ] #2 Given no session When a test calls each of getLibrary, createLibrary, renameLibrary, deleteLibrary, selectLibrary, removeSource, startScan, getScanStatus, importRekordboxXml, listImportedPlaylists, importPlaylist, deleteImportedPlaylist and getImportedPlaylistTracks Then each answers 401 NOT_SIGNED_IN and the operation does not run.
- [ ] #3 Given no session When a test calls each of matchTracks, fetchSpotifyPlaylist, listPreferences, rememberPreference, forgetAllPreferences, forgetPreference, exportPlaylist, exportMissing and exportSkipped Then each answers 401 NOT_SIGNED_IN and the operation does not run.
- [ ] #4 Given a couple portal session of wedding A When it calls each operation named in the first three criteria, the wedding-scoped operations with the id of A Then each answers 403 and writes no row: listDjWeddings, getDjWedding, getDjSongLists and getWeddingChanges with {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} from the account check (AppIdentity.java:81-87), and every other operation with the same body from the route guard in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02); 404 NO_WEDDING for another business's wedding stays asserted by P3-E01-T02.
- [ ] #5 Given a member holding only the ADMIN role who fills no team slot, in a business with weddings A and B that are not deleted When the admin calls getDjWedding, getDjSongLists, getWeddingChanges, getCoupleExportSummary, exportCoupleRekordboxXml and exportCoupleMissing for A and for B Then each answers as it does for a DJ assigned to that wedding, never 404 NO_WEDDING (deviation UD-14.b3; rekord-api answers 404 NO_WEDDING).
- [ ] #6 Given the same admin and a wedding of another business When the admin calls each wedding operation of the first criterion that takes a wedding id Then each answers 404 NO_WEDDING with the message "There is no such wedding." (CouplesResource.java:140).
- [ ] #7 Given a member holding only the ADMIN role When the admin calls each operation of the second and third criteria on the libraries of the business Then none answers 403, and the libraries the admin sees and acts on are the ones a planner sees (P3-E02-T02; deviation UD-14.b3; rekord-api's guard admits only DJ and PLANNER).
- [ ] #8 Given a member holding only the DJ role who fills no slot of wedding A When the DJ calls each wedding operation of the first criterion that takes a wedding id for A Then each answers 404 NO_WEDDING, so the existence of A cannot be probed (CouplesResource.java:138-141).
- [ ] #9 Given the matrix test When it runs Then it holds one row per operation of the first three criteria and one column for each of no session, couple portal session, DJ, PLANNER and ADMIN, and each row runs on a fresh fixture that makes the operation succeed for a DJ, named per row in its fixture table: a scanned library with one DRM-skipped file for exportSkipped, one stored preference for forgetPreference, one imported playlist for getImportedPlaylistTracks and deleteImportedPlaylist, a disposable library and source for deleteLibrary and removeSource, the WireMock stub of P3-E06-T06 for fetchSpotifyPlaylist, and a body naming one track for exportPlaylist, exportMissing and matchTracks.
- [ ] #10 Given a row of the matrix test When it runs for no session, a couple portal session, DJ and PLANNER Then it expects 401 NOT_SIGNED_IN for no session, 403 with the body criterion 4 names for the couple portal session (wedding-scoped rows use the session's own wedding), and for DJ and PLANNER the operation's success status (200, 201 for createLibrary, 202 for startScan) on a wedding where they fill a PENCILLED or CONFIRMED slot and on a library they own (PLANNER also on an unowned library), and 404 NO_WEDDING on a wedding of the business where they fill no slot.
- [ ] #11 Given a row of the matrix test When it runs for ADMIN Then it expects the operation's success status on every wedding of the business and on every library a planner sees (deviation UD-14.b3; rekord-api answers 404 NO_WEDDING to the wedding operations and the role-denied 403 to the library, matching and export operations, LibraryResource.java:41, MatchingResource.java:52, ExportsResource.java:46).
- [ ] #12 Given an operationId of paths/dj.yaml in the pinned contract that has no row in the matrix When the matrix test runs Then it fails and names that operationId.
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
