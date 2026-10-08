---
id: TASK-11.5
title: >-
  P1-E05-T05 Assign a team role, granting the named account access
  (assignTeamRole)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-11.4
  - TASK-9.3
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:189-221'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorMapper.java:50-58'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:133-158'
  - 'rekord-api/src/test/java/app/rekord/PlannerDomainTest.java:200-224'
  - 'rekord-contract/components/planner.yaml:242-342'
  - 'rekord-contract/paths/planner.yaml:245-282'
  - 'docs/rewrite/analysis/11-planner-weddings.md:115-116'
  - 'docs/rewrite/analysis/11-planner-weddings.md:150-151'
  - 'docs/rewrite/analysis/11-planner-weddings.md:391'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10505
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to fill a team role with a vendor, a contact, a named person or a member's account so that the right DJ gets access to the wedding and the vendor's bookings are counted.

Builds the operation assignTeamRole. Oracle: WeddingsResource.assignTeamRole (WeddingsResource.java:189-221). The slot of the role is replaced wholesale: state, vendor_id, contact_id, user_id, person_name and note all come from the body, a missing field clearing the stored one; a role without a slot (OTHER) gets one created at sort order 0; the answer is the TeamList of the wedding (BR-PL-31). The user_id of a PENCILLED or CONFIRMED slot of any role grants that account the read access of BR-PL-03 (BR-PL-32, RISK-43); the backend accepts it, the planner app not sending it is outside this backlog. Slots refer to vendors by id, so renaming a vendor keeps the assignment (BR-PL-51), and booking_count of a vendor counts its slots on weddings without deleted_at, in any state, cancelled weddings included and not distinct per wedding (BR-PL-50). Kept as in rekord-api: a PUT that sends only state PENCILLED and a vendor over a CONFIRMED slot with an account wipes the account, the contact, the person name and the note, and the account loses access (RISK-21, PIN-11-0391; the user kept the whole-slot replacement, UD-19.h); the planner form that sends this is outside this backlog. The checks that rekord-api lacks (the COMPLETED rule on a slot, the slot constraints and the business of the ids) are P1-E05-T06.

- Builds: `assignTeamRole`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-31, BR-PL-32, BR-PL-50, BR-PL-51, RISK-21, RISK-43, PIN-11-0391, UD-19.h
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E05-T05` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and a DJ member who does not see it When a planner calls assignTeamRole with role DJ, state CONFIRMED, user_id of the DJ and note "load-in side door 19:45" Then the answer is 200 with the 5 slots and the DJ slot CONFIRMED with that user_id and note, and the DJ's getWedding on it answers 200 (BR-PL-32, RISK-43).
- [ ] #2 Given that wedding with a member holding only DJ named on its DJ slot as CONFIRMED When a planner calls assignTeamRole with role MC, state PENCILLED and user_id of that DJ, and then with role DJ and state OPEN Then getWedding by the DJ answers 200 (BR-PL-03, BR-PL-32).
- [ ] #3 Given that wedding When a planner calls assignTeamRole with role OTHER, state NOT_NEEDED and person_name "Photo booth" Then the answer lists 6 slots with a new OTHER slot at sort order 0, and a second call with role OTHER replaces that slot instead of adding one (BR-PL-31).
- [ ] #4 Given the DJ slot CONFIRMED with a vendor, one of its contacts, the DJ member's user_id, person_name "Ray" and a note When a planner calls assignTeamRole with role DJ, state PENCILLED and the same vendor_id only Then the answer is 200 with the DJ slot PENCILLED, contact_id, user_id, person_name and note null, and the DJ member's getWedding on it answers 404 NO_WEDDING (kept as in rekord-api by UD-19.h, RISK-21, PIN-11-0391).
- [ ] #5 Given a vendor "Daan Vermeer" of the business When a planner assigns it to the DJ slot as CONFIRMED and then calls updateVendor to rename it "Daan Vermeer Music" Then getWeddingTeam shows the DJ slot with the same vendor_id and getVendor answers booking_count 1 (BR-PL-51).
- [ ] #6 Given that vendor on the DJ slot of a CANCELLED wedding, on the DJ and the OTHER slot of a second wedding as OPEN, and on the DJ slot of a third wedding with deleted_at set When a planner calls getVendor Then booking_count is 3 (BR-PL-50).
- [ ] #7 Given that wedding When a planner calls assignTeamRole for role DJ without state Then the answer is 422 VALIDATION_FAILED with the errors item {field state, code REQUIRED} (UD-19.d1), and the slot is unchanged.
- [ ] #8 Given an unknown id, a wedding of another business and a wedding with deleted_at set When a planner calls assignTeamRole with each Then each answer is 404 NO_WEDDING "There is no such wedding.", and a DJ member named on the wedding's DJ slot calling it gets 403 with the slot unchanged.
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
