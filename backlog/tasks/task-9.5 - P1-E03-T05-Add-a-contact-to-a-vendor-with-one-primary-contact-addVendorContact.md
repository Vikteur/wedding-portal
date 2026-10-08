---
id: TASK-9.5
title: >-
  P1-E03-T05 Add a contact to a vendor with one primary contact
  (addVendorContact)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
milestone: m-1
dependencies:
  - TASK-9.3
references:
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:126-139'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:177-206'
  - 'rekord-contract/components/planner.yaml:124-160'
  - 'rekord-contract/components/planner.yaml:219-238'
  - 'rekord-contract/paths/planner.yaml:714-735'
  - 'docs/rewrite/analysis/11-planner-weddings.md:149'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10305
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to add the people I deal with at a vendor and mark the main one so that I know whom to call.

Builds the operation addVendorContact. Oracle: VendorsResource.addVendorContact (VendorsResource.java:126-139, :192-206): the contact gets name, role, phone and e-mail from the body; is_primary true first demotes every other contact of the vendor, is_primary false or null leaves the new contact non-primary; the answer is 201 with the whole vendor and its contacts ordered by sort order, then name (BR-PL-49). The body is VendorContactInput with a name of at least 1 character required; e-mail is free text. An unknown vendor or one of another business is 404 NO_VENDOR "There is no such vendor.".

- Builds: `addVendorContact`
- Covers: BR-PL-49, BR-DM-15
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E03-T05` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a vendor without contacts When a planner calls addVendorContact with name "Petra", role "DAY LEAD", phone "+31 20 000 0000", email petra@example.com and is_primary true Then the answer is 201 with the vendor whose contacts hold Petra as primary, with user_id null.
- [ ] #2 Given that vendor When the planner adds the contact "Bram" with is_primary true Then the answer is 201, Bram is primary, Petra is no longer primary, and the contacts are listed as Bram then Petra (BR-PL-49, BR-DM-15).
- [ ] #3 Given a body without name, or with name "" When a planner calls addVendorContact Then the answer is 422 VALIDATION_FAILED with the errors item {field name, code REQUIRED} for the missing name and {field name, code TOO_SHORT} for the name "", and no contact is added (UD-19.d1).
- [ ] #4 Given an unknown vendor id and a vendor of another business When a planner calls addVendorContact with each Then each answer is 404 NO_VENDOR "There is no such vendor." and no contact is added.
- [ ] #5 Given a vendor with archived_at set directly by the test When a planner calls addVendorContact on it Then the answer is 201 and the vendor stays archived.
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
