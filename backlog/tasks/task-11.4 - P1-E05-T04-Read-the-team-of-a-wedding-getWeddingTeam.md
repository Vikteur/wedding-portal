---
id: TASK-11.4
title: P1-E05-T04 Read the team of a wedding (getWeddingTeam)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-10.2
  - TASK-9.5
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:182-187'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:296-300'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:250-254'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:117-131'
  - rekord-api/src/main/java/app/rekord/domain/WeddingTeam.java
  - 'rekord-contract/components/planner.yaml:242-342'
  - 'rekord-contract/components/planner.yaml:794-802'
  - 'rekord-contract/paths/planner.yaml:226-245'
  - 'docs/rewrite/analysis/11-planner-weddings.md:118'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10504
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to see every team slot of a wedding with its state and assignment so that I know which roles are still open.

Builds the operation getWeddingTeam. Oracle: WeddingsResource.getWeddingTeam (WeddingsResource.java:182-187, :296-300), WeddingService.teamOf (WeddingService.java:250-254) and WeddingMapper.teamSlot (WeddingMapper.java:117-131). The slots are ordered by role as text (CATERING, DJ, MC, OTHER, PHOTO, VENUE), then sort order. Each slot carries id, role, state, vendor_id, contact_id, user_id, person_name, note and historical; historical is true only when a contact snapshot is stored, and nothing stores one, so it is false (BR-PL-34). Deviation UD-19.i1 (RISK-46): vendor_name is the name of the slot's vendor, archived vendors included, and contact_name, phone and email are the name, phone and e-mail of the slot's contact; a slot without a vendor has vendor_name null and a slot without a contact has the other three null, so a slot that names only an account or a person has all four null; rekord-api leaves all four null on every slot. The wedding is found with the visibility rule of BR-PL-03, so a DJ named on a PENCILLED or CONFIRMED slot reads the team of that wedding.

- Builds: `getWeddingTeam`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-34, UD-19.i1
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E05-T04` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 just created When a planner calls getWeddingTeam Then the answer is 200 with 5 slots CATERING, DJ, MC, PHOTO and VENUE, each OPEN with vendor_id, contact_id, user_id, person_name and note null and historical false (BR-PL-34).
- [ ] #2 Given that wedding with an OTHER slot assigned and its DJ slot CONFIRMED with the vendor "Daan Vermeer" of the business, its contact Petra with phone "+31 20 000 0000" and email petra@example.com, and person_name "Ray" When a planner calls getWeddingTeam Then the slots read CATERING, DJ, MC, OTHER, PHOTO, VENUE, and the DJ slot carries the vendor_id and contact_id with vendor_name "Daan Vermeer", contact_name "Petra", phone "+31 20 000 0000" and email petra@example.com (deviation UD-19.i1, RISK-46; rekord-api answers null for all four).
- [ ] #3 Given that wedding with an OTHER slot assigned and a DJ member named on its PHOTO slot as CONFIRMED When the DJ calls getWeddingTeam Then the answer is 200 with the 6 slots CATERING, DJ, MC, OTHER, PHOTO, VENUE (BR-PL-03).
- [ ] #4 Given an unknown id, a wedding of another business, a wedding with deleted_at set and, for a DJ, a wedding naming the DJ only on an OPEN slot When the caller calls getWeddingTeam with each Then each answer is 404 NO_WEDDING "There is no such wedding.".
- [ ] #5 Given that wedding with its PHOTO slot PENCILLED with the archived vendor "Lens & Co" and no contact, and its MC slot CONFIRMED with only the user_id of a member When a planner calls getWeddingTeam Then the PHOTO slot has vendor_name "Lens & Co" with contact_name, phone and email null, and the MC slot has vendor_name, contact_name, phone and email null (UD-19.i1).
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
