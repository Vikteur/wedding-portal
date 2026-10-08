---
id: TASK-11.6
title: >-
  P1-E05-T06 Refuse team assignments that break the COMPLETED rule, a slot
  constraint or the business
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
  - TASK-10.4
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:189-221'
  - 'rekord-api/src/main/resources/db/migration/V4__weddings.sql:89-131'
  - >-
    rekord-api/src/main/resources/db/migration/V11__relax_assignment_constraints.sql:35-44
  - 'docs/rewrite/STATUS.md:261-278'
  - 'docs/rewrite/analysis/11-planner-weddings.md:116-117'
  - 'docs/rewrite/analysis/11-planner-weddings.md:385-405'
  - 'docs/rewrite/analysis/10-identity-access.md:291-292'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10506
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want a team assignment that breaks a rule refused with a clear message so that a finished wedding keeps its DJ and photographer and no slot points into another business.

Adds three refusals to assignTeamRole of P1-E05-T05, each a 422 VALIDATION_FAILED whose message names every violation and whose errors list has one item per violation with code INVALID_VALUE (UD-12, UD-19.d1). First, deviation UD-11.b: on a COMPLETED wedding the DJ or the PHOTO slot cannot be set to OPEN (field team.DJ or team.PHOTO); NOT_NEEDED, PENCILLED and CONFIRMED stay allowed, and other roles are free. Second, the slot constraints that rekord-api leaves to the database and answers with 500 (BR-PL-33, BR-DM-19): a contact_id without a vendor_id (field contact_id) and a CONFIRMED slot without a vendor_id, user_id or person_name (field state). Third, deviation RISK-11: vendor_id names a vendor of the caller's business, archived ones included (field vendor_id); contact_id names a contact of a vendor of the caller's business (field contact_id); user_id names an account with a membership in the caller's business (field user_id). rekord-api stores such an id unchecked (BR-DM-46); the check is decided (41-domain-model D-4, architecture-conventions §6.1); the refusal is this 422 VALIDATION_FAILED naming the field, and an id that does not exist gets the identical answer (deviation UD-19.i2). As in rekord-api, the account is not required to hold the DJ role and the contact is not required to belong to the named vendor (BR-PL-32). The other body ids of phase 1 are checked in P1-E09-T04.

- STOP (human approval in the pull request): auth-access
- Covers: UD-11.b, BR-PL-33, BR-DM-19, PIN-15-0080, RISK-11, UD-19.i2

Plan item `P1-E05-T06` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COMPLETED wedding with venue name "De Oude Tuinderij", its DJ slot CONFIRMED and its PHOTO slot PENCILLED When a planner calls assignTeamRole with role DJ and state OPEN Then the answer is 422 VALIDATION_FAILED naming the DJ slot with one errors item {field team.DJ, code INVALID_VALUE}, and the slot stays CONFIRMED (deviation UD-11.b).
- [ ] #2 Given that wedding When a planner calls assignTeamRole with role PHOTO and state OPEN Then the answer is 422 VALIDATION_FAILED with one errors item {field team.PHOTO, code INVALID_VALUE}, and the slot stays PENCILLED (deviation UD-11.b).
- [ ] #3 Given that wedding When a planner calls assignTeamRole with role PHOTO and state NOT_NEEDED, then role CATERING and state OPEN, then role DJ and state PENCILLED with person_name "Ray" Then every answer is 200 and the wedding stays COMPLETED (UD-11.a, UD-11.c).
- [ ] #4 Given a DRAFT wedding When a planner calls assignTeamRole with role DJ and state OPEN Then the answer is 200, because the rule binds only a COMPLETED wedding.
- [ ] #5 Given a vendor of the business with a contact When a planner calls assignTeamRole with role CATERING, state PENCILLED and that contact_id without vendor_id Then the answer is 422 VALIDATION_FAILED with the errors item {field contact_id, code INVALID_VALUE}, and the slot is unchanged (deviation UD-12; rekord-api answers 500, BR-PL-33).
- [ ] #6 Given a wedding When a planner calls assignTeamRole with role DJ and state CONFIRMED and no vendor_id, user_id or person_name Then the answer is 422 VALIDATION_FAILED with the errors item {field state, code INVALID_VALUE}, and a call with state CONFIRMED and only a user_id of a member of the business answers 200 (deviation UD-12; rekord-api answers 500, BR-DM-19, PIN-15-0080).
- [ ] #7 Given a vendor, a contact of that vendor and a member account, all of another business When a planner calls assignTeamRole with role DJ and state PENCILLED naming each in turn as vendor_id, contact_id with an own vendor_id, and user_id Then each answer is 422 VALIDATION_FAILED with the errors item {field vendor_id, code INVALID_VALUE}, {field contact_id, code INVALID_VALUE} or {field user_id, code INVALID_VALUE}, and the slot is unchanged (deviation RISK-11, UD-19.i2; rekord-api stores the id).
- [ ] #8 Given an archived vendor of the business, a contact of another vendor of the business and an account of the business holding only PLANNER When a planner calls assignTeamRole with role PHOTO and state CONFIRMED naming them Then the answer is 200 with those ids stored (BR-PL-32).
- [ ] #9 Given a COMPLETED wedding whose DJ slot is CONFIRMED When a planner calls assignTeamRole with role DJ, state OPEN and a vendor_id of another business Then the answer is 422 VALIDATION_FAILED whose errors list holds {field team.DJ, code INVALID_VALUE} and {field vendor_id, code INVALID_VALUE}, in that order (UD-12, UD-19.i2).
- [ ] #10 Given a wedding and three random UUIDs that name no vendor, contact or account When a planner calls assignTeamRole with role DJ and state PENCILLED naming each in turn as vendor_id, as contact_id with a vendor_id of the business, and as user_id Then each answer is 422 VALIDATION_FAILED with the errors item {field vendor_id, code INVALID_VALUE}, {field contact_id, code INVALID_VALUE} or {field user_id, code INVALID_VALUE}, identical to the answer for an id of another business, and the slot is unchanged (deviation UD-19.i2; rekord-api fails on the foreign key with 500 UNKNOWN).
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
