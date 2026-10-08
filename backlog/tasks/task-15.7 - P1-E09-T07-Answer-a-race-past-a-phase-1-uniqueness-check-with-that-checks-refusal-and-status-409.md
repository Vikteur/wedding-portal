---
id: TASK-15.7
title: >-
  P1-E09-T07 Answer a race past a phase-1 uniqueness check with that check's
  refusal and status 409
status: To Do
assignee: []
created_date: '2026-10-07 07:13'
labels:
  - technical
  - P1
milestone: m-1
dependencies:
  - TASK-8.6
  - TASK-9.3
  - TASK-10.2
  - TASK-8.8
  - TASK-11.1
  - TASK-11.2
references:
  - 'rekord-api/src/main/java/app/rekord/error/ErrorMappers.java:109-121'
  - 'rekord-api/src/main/resources/db/migration/V1__identity.sql:94-96'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:107'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:440'
  - 'docs/rewrite/analysis/41-domain-model-and-glossary.md:1608-1616'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-15
priority: high
type: task
ordinal: 10907
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want a unique-constraint clash on a phase-1 table answered with the refusal of the domain check it slipped past so that a race gives the client the same readable answer as a sequential duplicate and no table or index name reaches a client.

Deviation UD-19.i5 (RISK-26, BR-DM-41, PIN-15-0107, PIN-15-0440): in rekord-api a unique-constraint violation reaches the catch-all and answers 500 UNKNOWN. wedding-portal answers a clash on a unique index that a domain check guards with status 409 and the code, message and errors item of that domain check. Exactly four unique indexes are translated, each by the persistence adapter of the ticket that owns its domain check, which adds the race test: the adapter turns the clash into the refusal of that check, and the error mapper of P0-E05 answers it with 409: ux_dj_invites_open with "email already has an open invitation" (P1-E02-T06), ux_users_email with "email already has an account" (P1-E02-T08), ux_vendors_name with "name is already used by a vendor of this category" (P1-E03-T03), and ux_wedding_people_partner_slot with "sort_order is already used by another partner of this wedding" (P1-E05-T01, P1-E05-T02). Each answers 409 VALIDATION_FAILED with one errors item of code INVALID_VALUE. The refusals of ux_dj_invites_open, ux_vendors_name and ux_wedding_people_partner_slot name the field of their check (email, name and sort_order); the refusal of ux_users_email carries field null, because the acceptInvite body holds no address and the address comes from the invite (UD-19.d1, P1-E02-T08). ux_weddings_slug is not translated: createWedding picks a free slug instead of refusing (P1-E04-T02), so a slug race keeps 500 UNKNOWN "Something went wrong at our end.". A clash on any other constraint keeps 500 UNKNOWN as P0-E05-T05 states. The library-name races of createLibrary and renameLibrary named by UD-19.i5 are phase 3. The 409 answer keeps the Error schema of the contract; only its status differs from the 422 of the sequential duplicate.

- Covers: RISK-26, PIN-15-0107, PIN-15-0440, UD-19.i5

Plan item `P1-E09-T07` (technical,P1) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the persistence adapters of phase 1 When the test lists the unique indexes whose clash an adapter turns into a domain refusal Then the list is exactly ux_dj_invites_open, ux_users_email, ux_vendors_name and ux_wedding_people_partner_slot, each with the code VALIDATION_FAILED, the field and the message of its domain check, the field being email, null, name and sort_order in that order (deviation UD-19.i5, RISK-26, UD-19.d1).
- [ ] #2 Given the 409 answers of the race tests of P1-E02-T06, P1-E02-T08, P1-E03-T03, P1-E05-T01 and P1-E05-T02 When the test reads their bodies Then none holds a table, index, column or SQL state name, and each is valid against the Error schema of the contract (PIN-15-0440).
- [ ] #3 Given a planner and no wedding When the test makes two createWedding calls with couple_display_name "Emma & Julian" pass the free-slug check before either stores its row, using a test latch in the wedding repository adapter Then one answer is 201 and the other is 500 {"detail":{"code":"UNKNOWN","message":"Something went wrong at our end."}} with no index name in the body, and one wedding with the slug emma-julian is stored (BR-DM-41, UD-19.i5).
- [ ] #4 Given a vendor "Daan Vermeer" of category DJ When a planner calls createVendor with the same category and the name "daan vermeer" Then the answer is the 422 VALIDATION_FAILED of P1-E03-T03 with the errors item {field name, code INVALID_VALUE}, and the vendors table still holds exactly one row for that business, category and lower-cased name (PIN-15-0440).
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
