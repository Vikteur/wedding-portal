---
id: TASK-10.5
title: P1-E04-T05 Delete a wedding softly or purge it (deleteWedding)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:126-130'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:217-240'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalGate.java:100-137'
  - 'rekord-contract/paths/planner.yaml:42-110'
  - 'docs/rewrite/analysis/11-planner-weddings.md:95'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:99'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-10
priority: high
type: feature
ordinal: 10405
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to remove a wedding, and erase it completely when asked, so that cancelled bookings leave the overview and an erasure request removes every trace.

Builds the operation deleteWedding. Oracle: WeddingsResource.deleteWedding (WeddingsResource.java:126-130) and WeddingService.delete (WeddingService.java:221-240). Without purge, or with purge false, the wedding gets deleted_at and every live portal of it gets revoked_at; open portal sessions are not revoked, because the portal gate refuses a deleted wedding (BR-PL-21, BR-DM-33, BR-CP-32). With purge true the row is removed and the schema's cascades remove its people, team slots, song lists, portals and portal sessions; the timeline and task cascades come with their tables in P1-E07-T01 and P1-E08-T01. A deleted wedding is invisible to every lookup, so a second delete or a purge after a soft delete answers 404 NO_WEDDING "There is no such wedding." (BR-DM-38). The answer is 204. rekord-api has no test for this operation, so the ticket is test-first (UD-15.d).

- Builds: `deleteWedding`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-PL-21, BR-DM-33, BR-DM-38, BR-CP-32, PIN-15-0099
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E04-T05` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for deleteWedding When a characterization test calls deleteWedding against rekord-api without purge, with purge true, a second time on the same wedding and for an unknown id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and a PORTAL session row on its COUPLE portal When a planner calls deleteWedding without purge Then the answer is 204, getWedding answers 404 NO_WEDDING "There is no such wedding.", listWeddings with status all no longer lists it, the row still exists with deleted_at set, both portals have revoked_at set and the session row keeps revoked_at null (BR-DM-33, PIN-15-0099, BR-CP-32).
- [ ] #3 Given the same wedding with its FRIENDS portal revoked a day earlier When a planner calls deleteWedding with purge false Then the answer is 204 and the FRIENDS portal keeps its earlier revoked_at.
- [ ] #4 Given the same wedding before any delete When a planner calls deleteWedding with purge true Then the answer is 204 and the wedding row, its people, team slots, song lists, portals and the PORTAL session row are gone (BR-PL-21).
- [ ] #5 Given a wedding deleted without purge When a planner calls deleteWedding on it again, once without purge and once with purge true Then both answers are 404 NO_WEDDING "There is no such wedding." and the row still exists with its first deleted_at (BR-DM-38).
- [ ] #6 Given a wedding "Emma & Julian" with slug emma-julian deleted without purge When a planner calls createWedding with "Emma & Julian" Then the new wedding gets slug emma-julian.
- [ ] #7 Given an unknown id and a wedding of another business When a planner calls deleteWedding with each Then each answer is 404 NO_WEDDING "There is no such wedding." and the other business's wedding is unchanged, and a member holding only DJ gets 403.
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
