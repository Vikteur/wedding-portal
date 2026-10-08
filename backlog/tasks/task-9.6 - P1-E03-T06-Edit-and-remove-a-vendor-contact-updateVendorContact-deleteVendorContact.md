---
id: TASK-9.6
title: >-
  P1-E03-T06 Edit and remove a vendor contact (updateVendorContact,
  deleteVendorContact)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-9.5
references:
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:141-175'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:192-206'
  - 'rekord-contract/paths/planner.yaml:737-780'
  - 'docs/rewrite/analysis/11-planner-weddings.md:149'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:76'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10306
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to fix a contact's details and remove a contact who left so that the directory never sends me to the wrong person.

Builds the operations updateVendorContact and deleteVendorContact. Oracle: VendorsResource.updateVendorContact and deleteVendorContact (VendorsResource.java:141-175, :192-206): update replaces name, role, phone and e-mail from the body, is_primary true demotes the other contacts, false clears the flag, null keeps it, and the answer is the whole vendor; delete removes the row for good and answers 204 (BR-PL-49). An unknown contact or one of another business is 404 NO_VENDOR "There is no such contact.". rekord-api has no test for either operation, so the ticket is test-first (UD-15.d).

- Builds: `updateVendorContact`, `deleteVendorContact`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-PL-49, PIN-15-0076
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E03-T06` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for updateVendorContact When a characterization test calls updateVendorContact against rekord-api with a new name, with is_primary true, false and null, and for an unknown contact id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given rekord-api as the behaviour oracle and no rekord-api test for deleteVendorContact When a characterization test calls deleteVendorContact against rekord-api for a contact of the business, an unknown contact id and a contact of another business Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #3 Given a vendor with the contacts Petra (primary) and Bram When a planner calls updateVendorContact on Bram with name "Bram V", no role and is_primary true Then the answer is 200 with the vendor whose contacts show "Bram V" as primary with role null and Petra as not primary (BR-PL-49).
- [ ] #4 Given that vendor When the planner calls updateVendorContact on "Bram V" again with name "Bram V" and is_primary true Then the answer is 200 and "Bram V" is still the only primary contact (PIN-15-0076).
- [ ] #5 Given that vendor When the planner calls updateVendorContact on Petra with name "Petra" and is_primary null, and then on "Bram V" with name "Bram V" and is_primary false Then Petra stays not primary after the first call and the vendor has no primary contact after the second.
- [ ] #6 Given a contact of the business When a planner calls deleteVendorContact with its id Then the answer is 204, getVendor no longer lists it and the row is gone.
- [ ] #7 Given an unknown contact id and a contact of another business When a planner calls updateVendorContact and deleteVendorContact with each Then every answer is 404 NO_VENDOR "There is no such contact." and the other business's contact is unchanged.
- [ ] #8 Given a body with name "" When a planner calls updateVendorContact Then the answer is 422 VALIDATION_FAILED with the errors item {field name, code TOO_SHORT} and the contact is unchanged (UD-19.d1).
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
