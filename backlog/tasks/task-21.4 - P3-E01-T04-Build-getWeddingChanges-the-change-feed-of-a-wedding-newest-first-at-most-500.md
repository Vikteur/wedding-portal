---
id: TASK-21.4
title: >-
  P3-E01-T04 Build getWeddingChanges: the change feed of a wedding, newest
  first, at most 500
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-21.3
references:
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:117-135'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:233-240'
  - 'rekord-contract/components/common.yaml:209-217'
  - 'rekord-contract/paths/dj.yaml:88-104'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:430'
  - 'docs/rewrite/analysis/20-unmerged-and-python-only-work.md:48'
  - 'rekord-api/src/main/java/app/rekord/portal/SongService.java:218-231'
  - 'rekord-api/src/main/resources/db/migration/V6__music.sql:97-100'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-21
priority: high
type: feature
ordinal: 30104
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to see what the couple and their friends changed most recently so that I notice new wishes without rereading every list.

The change feed lists the song-list edits of the couple and their friends, attributed by token kind (UX-04). rekord-api reads the newest rows first, with limit 100 when none is given and never more than 500 rows (SongService.java:238); the contract's minimum of 1 is enforced only by the generated validation and no rekord-api test pins it (BR-MX-44, PIN-14-0430). The portal-link rows of UX-03 are added to this feed by P1-E06. Visibility is the assignment rule of P3-E01-T01.

- Builds: `getWeddingChanges`
- STOP (human approval in the pull request): auth-access
- Covers: UX-04, BR-MX-44, PIN-14-0430, PIN-14-0508, PIN-20-0131, BR-CP-24, BR-CP-39, BR-DM-28, PIN-12-0210, PIN-15-0089
- Error code `FORBIDDEN` without a criterion here: answered by the account check of the use case with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} to a session that carries no member account, such as a couple portal session, as rekord-api answers (AppIdentity.java:81-87, ErrorMappers.java:67-73); asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E01-T04` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an assigned wedding with 150 change rows When the DJ calls getWeddingChanges without limit Then the answer is 200 with the 100 newest rows, newest first by id descending, each with token_kind, action, kind, uid, summary and at as rekord-api maps them (CouplesResource.java:117-135).
- [ ] #2 Given an assigned wedding with 600 change rows When the DJ calls getWeddingChanges with limit=1000 Then the answer holds exactly 500 rows, the 500 newest (PIN-20-0131, PIN-14-0508).
- [ ] #3 Given an assigned wedding with 600 change rows When the DJ calls getWeddingChanges with limit=20 Then the answer holds the 20 newest rows.
- [ ] #4 Given an assigned wedding When the DJ calls getWeddingChanges with limit=0 and with limit=-1 Then each answers 422 VALIDATION_FAILED in the error envelope (PIN-14-0430, PIN-14-0508).
- [ ] #5 Given a wedding the DJ is not assigned to and an unknown wedding id When the DJ calls getWeddingChanges with each Then each answers 404 NO_WEDDING with the message "There is no such wedding.".
- [ ] #6 Given a member holding only the ADMIN role who fills no slot of a wedding of the business with 3 change rows When the admin calls getWeddingChanges for it Then the answer is 200 with the 3 rows (deviation UD-14.b3; rekord-api answers 404 NO_WEDDING).
- [ ] #7 Given an assigned wedding with a change row stored with source friend, action added and kind couple_top20, followed by a row stored with source couple, action details and no kind When the DJ calls getWeddingChanges Then the answer lists the details row first with token_kind couple, action details and kind null, then the added row with token_kind friend, action added and kind couple_top20 (BR-CP-39; CouplesResource.java:126-136).
- [ ] #8 Given an assigned wedding with 3 change rows and a putPortalEntry whose transaction the test hook of P2-E05-T02 criterion 7 makes fail after its song_changes row was flushed When the DJ calls getWeddingChanges Then the answer holds the same 3 rows and no row for the failed write, because the change row is written in the same transaction as the entry (BR-DM-28, PIN-15-0089; SongService.java:218-231).
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
