---
id: TASK-9.2
title: >-
  P1-E03-T02 Browse and search the vendor directory and open one vendor
  (listVendors, getVendor)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
milestone: m-1
dependencies:
  - TASK-9.1
  - TASK-7.8
references:
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:31-102'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:160-182'
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorMapper.java:20-58'
  - 'rekord-contract/paths/planner.yaml:600-675'
  - 'docs/rewrite/analysis/11-planner-weddings.md:145-146'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10302
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to browse the directory by category, search it and open one vendor with its contacts so that I find the right supplier for a wedding.

Builds the operations listVendors and getVendor. Oracle: VendorsResource.listVendors and getVendor (VendorsResource.java:45-102) with VendorMapper (VendorMapper.java:20-58): archived vendors are hidden unless include_archived is true; category filters; q matches name, place and blurb of the vendor and name, role, e-mail and phone of any of its contacts as a case-insensitive substring; the list is ordered by lower-cased name with no paging (BR-PL-45). getVendor returns an archived vendor too, with archived true (BR-PL-46). Contacts are ordered by sort order, then name. booking_count counts the team slots that name the vendor on weddings without deleted_at, in any state (VendorMapper.java:50-58); the team slots exist from P1-E05-T05 on, and until then it is 0. An unknown id or a vendor of another business is 404 NO_VENDOR "There is no such vendor.".

- Builds: `listVendors`, `getVendor`
- Covers: BR-PL-45
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E03-T02` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the CATERING vendors "Zuid Catering" and "aroma" and the PHOTO vendor "Beeld" of the business, and an archived CATERING vendor "Oud" When a planner calls listVendors with category CATERING Then the answer is 200 with "aroma" and "Zuid Catering" in that order, and with include_archived true the answer is "aroma", "Oud" (archived true), "Zuid Catering" in that order (BR-PL-45).
- [ ] #2 Given a vendor "Beeld" whose contact has the role "allergies lead" When a planner calls listVendors with q "ALLERG" Then the answer holds "Beeld", and q "beel" also finds it by its name (BR-PL-45).
- [ ] #3 Given a vendor of the business with two contacts "Yara" and "Bram" When a planner calls getVendor with its id Then the answer is 200 with id, category, name, place, blurb, website, booking_count 0, archived false and the contacts "Bram" then "Yara".
- [ ] #4 Given an archived vendor When a planner calls getVendor with its id Then the answer is 200 with archived true (BR-PL-46).
- [ ] #5 Given an unknown vendor id and a vendor of another business When a planner calls getVendor with each Then each answer is 404 NO_VENDOR "There is no such vendor.", and listVendors never lists the other business's vendor.
- [ ] #6 Given a member holding only DJ When the member calls listVendors or getVendor Then both answers are 403.
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
