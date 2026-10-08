---
id: TASK-11.3
title: P1-E05-T03 Remove a person from a wedding (deletePerson)
status: To Do
assignee: []
created_date: '2026-10-07 07:12'
labels:
  - user-story
  - P1
  - test-first
milestone: m-1
dependencies:
  - TASK-11.1
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:176-180'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:261-268'
  - 'rekord-contract/paths/planner.yaml:208-226'
  - 'docs/rewrite/analysis/15-data-model-persistence.md:103'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-11
priority: high
type: feature
ordinal: 10503
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want to remove a person from a wedding so that the list holds only the people who take part.

Builds the operation deletePerson. Oracle: WeddingsResource.deletePerson (WeddingsResource.java:176-180) with requirePerson (WeddingsResource.java:261-268): the row is deleted outright, there is no soft delete and no undo (BR-DM-37), and the answer is 204. Removing a PARTNER changes neither the portals' code nor code_stale, as in rekord-api. Not found and not visible answer as for updatePerson (BR-PL-04). rekord-api has no test for this operation, so the ticket is test-first (UD-15.d).

- Builds: `deletePerson`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-DM-37, PIN-15-0103
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E05-T03` (user-story,P1,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and no rekord-api test for deletePerson When a characterization test calls deletePerson against rekord-api on a FAMILY person, on a PARTNER, a second time on the same person and for an unknown id Then it records each status, error code and body, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a CONFIRMED wedding of the partners Emma and Julian (display name "Emma & Julian") dated 2027-06-12 in time zone Europe/Amsterdam, with live COUPLE and FRIENDS portals and the access code EJ12062027 and the FAMILY person Ingrid When a planner calls deletePerson on Ingrid Then the answer is 204, listWeddingPeople no longer lists her and the database holds no wedding_people row with her id (BR-DM-37, PIN-15-0103).
- [ ] #3 Given that wedding When a planner calls deletePerson on the PARTNER Julian Then the answer is 204, the only PARTNER left is Emma, and both portals keep code EJ12062027 with code_stale false.
- [ ] #4 Given a person already deleted and an unknown id When a planner calls deletePerson with each Then each answer is 404 NO_WEDDING "There is no such person.".
- [ ] #5 Given a person on a wedding of another business and a person on a wedding with deleted_at set When a planner calls deletePerson on each Then each answer is 404 NO_WEDDING "There is no such wedding." and the person still exists, and a member holding only DJ named on the wedding's DJ slot gets 403.
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
