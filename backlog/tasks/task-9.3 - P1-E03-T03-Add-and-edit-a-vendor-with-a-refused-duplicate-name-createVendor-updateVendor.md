---
id: TASK-9.3
title: >-
  P1-E03-T03 Add and edit a vendor with a refused duplicate name (createVendor,
  updateVendor)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
milestone: m-1
dependencies:
  - TASK-9.2
references:
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:85-114'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:184-190'
  - 'rekord-api/src/main/resources/db/migration/V3__vendors.sql:24-27'
  - 'rekord-contract/components/planner.yaml:199-217'
  - 'rekord-contract/paths/planner.yaml:633-700'
  - 'docs/rewrite/analysis/11-planner-weddings.md:147-148'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:75'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10303
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to add a vendor to the directory and edit it so that the directory stays up to date and every wedding shows the new details.

Builds the operations createVendor and updateVendor. Oracle: VendorsResource.createVendor and updateVendor (VendorsResource.java:85-114, :184-190): createVendor answers 201 with the vendor; updateVendor replaces category, name, place, blurb and website from the body, so a field left out becomes null (BR-PL-48); weddings refer to a vendor by id, so a rename shows everywhere (BR-PL-51, asserted in P1-E05-T05). The body is VendorInput with category and a name of at least 1 character required. Deviation RISK-39 class, UD-12: a name already used by a vendor of the same business and category, compared case-insensitively and archived vendors included, is refused with 422 VALIDATION_FAILED "name is already used by a vendor of this category" instead of failing on the name index as 500 (BR-PL-47, PIN-15-0075). Two requests that race past that check clash on ux_vendors_name; the persistence adapter maps the clash to the same refusal, answered with 409 (deviation UD-19.i5, P0-E05-T05).

- Builds: `createVendor`, `updateVendor`
- Covers: BR-PL-47, BR-PL-48, BR-DM-14, PIN-15-0075, UD-19.i5
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P1-E03-T03` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a planner When the planner calls createVendor with category CATERING, name "Smaakmakers", place "'s-Hertogenbosch" and no blurb Then the answer is 201 with a new id, the place stored as entered, archived false, booking_count 0 and no contacts.
- [ ] #2 Given that vendor When the planner calls updateVendor with category CATERING, name "Smaakmakers Catering" and no place Then the answer is 200 with the new name and place null, and getVendor shows the same (BR-PL-48).
- [ ] #3 Given a CATERING vendor "Smaakmakers" When a planner calls createVendor with category CATERING and name "SMAAKMAKERS" Then the answer is 422 VALIDATION_FAILED "name is already used by a vendor of this category" with the errors item {field name, code INVALID_VALUE} and no vendor is added (deviation UD-12; rekord-api answers 500, PIN-15-0075).
- [ ] #4 Given that vendor with archived_at set directly by the test When a planner calls createVendor with category CATERING and name "Smaakmakers" Then the answer is 422 VALIDATION_FAILED "name is already used by a vendor of this category" with the errors item {field name, code INVALID_VALUE} (BR-PL-47).
- [ ] #5 Given the CATERING vendors "Aroma" and "Smaakmakers" When a planner calls updateVendor on "Aroma" with name "smaakmakers" Then the answer is 422 VALIDATION_FAILED "name is already used by a vendor of this category" with the errors item {field name, code INVALID_VALUE} and "Aroma" keeps its name, while the same name with category PHOTO answers 200.
- [ ] #6 Given a body without name, and one without category When a planner calls createVendor or updateVendor with each Then the answer is 422 VALIDATION_FAILED with the errors item {field name, code REQUIRED} for the first and {field category, code REQUIRED} for the second, and nothing changes (UD-19.d1).
- [ ] #7 Given an unknown vendor id and a vendor of another business When a planner calls updateVendor with each Then each answer is 404 NO_VENDOR "There is no such vendor." and the other business's vendor is unchanged.
- [ ] #8 Given no vendor named "Smaakmakers" and a test latch in the vendor repository adapter that holds both calls after the duplicate-name check When two planners call createVendor with category CATERING and name "Smaakmakers" at the same time Then one answers 201 and the other answers 409 VALIDATION_FAILED "name is already used by a vendor of this category" with the errors item {field name, code INVALID_VALUE}, and the business holds one CATERING vendor "Smaakmakers" (deviation UD-19.i5; rekord-api answers 500 UNKNOWN).
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
