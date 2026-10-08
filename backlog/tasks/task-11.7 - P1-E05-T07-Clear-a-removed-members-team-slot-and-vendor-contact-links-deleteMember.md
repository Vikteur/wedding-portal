---
id: TASK-11.7
title: >-
  P1-E05-T07 Clear a removed member's team-slot and vendor-contact links
  (deleteMember)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-11.5
  - TASK-8.4
  - TASK-8.8
references:
  - 'rekord-api/src/main/java/app/rekord/account/AccountsResource.java:83-96'
  - 'rekord-api/src/main/resources/db/migration/V4__weddings.sql:89-131'
  - 'rekord-api/src/main/resources/db/migration/V3__vendors.sql:42-66'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:411-414'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:102'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10507
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As an admin, I want removing a member to clear the team slots and vendor contacts that name the member so that no wedding or vendor keeps pointing at an account that is gone.

Deviation UD-19.i3 (RISK-34, BR-DM-36, PIN-15-0102): deleteMember of P1-E02-T04 also clears the links to the removed account, in the same transaction. Every wedding_team row of the business whose user_id names the account, and every vendor_contacts row of a vendor of the business whose user_id names the account, gets user_id null; their other fields stay. A CONFIRMED slot that names neither a vendor nor a person name first gets the account's display_name as its person_name, so the check ck_wedding_team_assigned still holds and the slot still shows who worked it. A refused deleteMember (403 FORBIDDEN, 400 LAST_ADMIN or 404 NO_USER) clears nothing. rekord-api keeps both links (AccountsResource.java:83-96). The foreign keys stay ON DELETE SET NULL for the hard delete of a purged account by the cleanup job of P1-E09-T09.

- STOP (human approval in the pull request): auth-access
- Covers: RISK-34, PIN-15-0102, BR-DM-36, UD-19.i3

Plan item `P1-E05-T07` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ member named on a wedding's DJ slot as CONFIRMED with a vendor of the business and the note "load-in side door 19:45", and linked as user to that vendor's contact Petra When an admin calls deleteMember for the DJ Then getWeddingTeam shows the DJ slot CONFIRMED with the same vendor_id and note and user_id null, and getVendor shows Petra with user_id null and her other fields unchanged (deviation UD-19.i3, RISK-34; rekord-api keeps both user_id values).
- [ ] #2 Given a DJ member with the display_name "Nova" named on a wedding's PHOTO slot as CONFIRMED with no vendor_id and no person_name When an admin calls deleteMember for the member Then the PHOTO slot stays CONFIRMED with person_name "Nova" and user_id null (UD-19.i3).
- [ ] #3 Given a member named on a wedding's MC slot as PENCILLED with no vendor_id and no person_name When an admin calls deleteMember for the member Then the MC slot stays PENCILLED with vendor_id, contact_id, user_id and person_name null (UD-19.i3).
- [ ] #4 Given a DJ removed while named on a wedding's DJ slot When the same address is invited again as DJ, the invite is accepted and the new account calls getWedding on that wedding Then the answer is 404 NO_WEDDING "There is no such wedding.", because no slot names the new account.
- [ ] #5 Given that wedding and that new account When a planner calls assignTeamRole with role DJ, state CONFIRMED and the new account's user_id Then the new account's getWedding on it answers 200.
- [ ] #6 Given a DJ member named on a wedding's DJ slot as CONFIRMED and a planner without ADMIN When the planner calls deleteMember for the DJ Then the answer is 403 FORBIDDEN "This is not yours to open." and the slot keeps the DJ's user_id (UD-14.d).
- [ ] #7 Given a team slot and a vendor contact naming an account When the repository test deletes the account row Then both rows remain with user_id null (BR-DM-36).
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
