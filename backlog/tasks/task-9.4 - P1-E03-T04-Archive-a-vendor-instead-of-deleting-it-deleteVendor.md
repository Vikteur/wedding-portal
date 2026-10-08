---
id: TASK-9.4
title: P1-E03-T04 Archive a vendor instead of deleting it (deleteVendor)
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
  - 'rekord-api/src/main/java/app/rekord/vendor/VendorsResource.java:116-124'
  - 'rekord-contract/paths/planner.yaml:700-712'
  - 'docs/rewrite/analysis/11-planner-weddings.md:146'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:100'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-9
priority: high
type: feature
ordinal: 10304
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to remove a vendor from the pickers so that I stop booking a supplier while past weddings keep showing who worked them.

Builds the operation deleteVendor. Oracle: VendorsResource.deleteVendor (VendorsResource.java:116-124): the vendor gets archived_at set to the time of the call, again on each call, and the answer is 204; deleted_at is never written and there is no way back from archived (BR-PL-46, BR-DM-34). An archived vendor is hidden from listVendors unless include_archived is true, is still returned by getVendor and still accepts updates and contacts, as in rekord-api.

- Builds: `deleteVendor`
- Covers: BR-PL-46, BR-DM-34
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E03-T04` (user-story,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a vendor of the business When a planner calls deleteVendor with its id Then the answer is 204, listVendors without include_archived no longer lists it, getVendor answers 200 with archived true, and the row still exists with archived_at set and deleted_at null (BR-DM-34).
- [ ] #2 Given an archived vendor When a planner calls deleteVendor with its id again Then the answer is 204 and archived_at is set to the time of the second call (BR-PL-46).
- [ ] #3 Given an archived vendor When a planner calls updateVendor on it Then the answer is 200 and the vendor stays archived.
- [ ] #4 Given an unknown vendor id and a vendor of another business When a planner calls deleteVendor with each Then each answer is 404 NO_VENDOR "There is no such vendor." and the other business's vendor stays unarchived.
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
