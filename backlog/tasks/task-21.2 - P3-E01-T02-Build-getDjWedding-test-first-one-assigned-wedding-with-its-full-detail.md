---
id: TASK-21.2
title: >-
  P3-E01-T02 Build getDjWedding test-first: one assigned wedding with its full
  detail
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - test-first
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-21.1
references:
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:72-77'
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:138-141'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:97-121'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:72-157'
  - 'rekord-contract/paths/dj.yaml:1-53'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:177-180'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-21
priority: high
type: feature
ordinal: 30102
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to open one wedding I am booked for so that I see its team, its run sheet and the couple's details in one place.

rekord-api has no test for this operation, so it is built test-first (UD-15.d). Visibility is the assignment rule of P3-E01-T01 (BR-MX-42): a wedding id the caller is not assigned to, of another business, soft-deleted or unknown answers 404 NO_WEDDING with rekord-api's message, so ids cannot be probed (rekord-contract/paths/dj.yaml:4-6). A planner is neither the couple nor their DJ and gets 404 here unless assigned (WeddingRepository.findAssigned). The answer is the planner projection rekord-api returns (BR-MX-45) with one change, decided by the user: the portal field is null, so neither the portal token, nor the portal url that ends in the token, nor the access code reaches the DJ, while the people keep their contact fields (deviation UD-19.m1, RISK-04). The contract's PortalLink requires url and code and Wedding.portal is nullable, so null is the contract-conform answer that drops both, and the contract is unchanged. A couple portal session carries a business but no account, so rekord-api finds the wedding in that business first and then refuses the missing account with 403, which lets such a session tell which wedding ids of its business exist (BR-CP-26); the ticket keeps that order.

- Builds: `getDjWedding`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- STOP (human approval in the pull request): auth-access
- Covers: BR-MX-42, BR-MX-45, BR-CP-26, BR-ID-23, RISK-04, UD-19.m1
- Error code `FORBIDDEN` without a criterion here: answered by the account check of the use case with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} to a session that carries no member account, such as a couple portal session, as rekord-api answers (AppIdentity.java:81-87, ErrorMappers.java:67-73); asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E01-T02` (user-story,P3,test-first,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle and a wedding with a DJ assigned PENCILLED When a characterization test calls getDjWedding as that DJ, as an unassigned DJ of the same business and with an unknown wedding id Then it records 200 with the Wedding detail, 404 NO_WEDDING and 404 NO_WEDDING, and the same test passes against wedding-portal before the operation counts as built, with the one difference that the portal field of the 200 answer is null there (UD-15.d; deviation UD-19.m1; rekord-api answers the COUPLE link).
- [ ] #2 Given a DJ who fills the DJ slot of wedding A in state CONFIRMED, and a COUPLE portal link and a FRIENDS portal link of A that are not revoked When the DJ calls getDjWedding for A Then the answer is 200 with the Wedding detail built from the wedding, its people with their contact fields, its team and its song lists, and portal null (BR-MX-45; deviation UD-19.m1; rekord-api answers portal with the COUPLE link: scope COUPLE, a url ending in /g/ plus the token, the code, code_stale, revoked false, expires_at and last_seen_at, WeddingMapper.java:72-157, WeddingService.java:265-270).
- [ ] #3 Given the same DJ and wedding A, whose COUPLE portal token is t and whose access code is c, and a person of A with email "person@example.com" and phone "+31600000000" When the DJ calls getDjWedding for A Then no string value of the JSON answer contains t, no value equals c, and that person carries email "person@example.com" and phone "+31600000000" (deviation UD-19.m1; rekord-api answers t inside portal.url and c as portal.code).
- [ ] #4 Given a DJ who fills no slot of wedding B of the same business When the DJ calls getDjWedding for B Then the answer is 404 NO_WEDDING with the message "There is no such wedding." in the error envelope.
- [ ] #5 Given a wedding id of another business, the id of a soft-deleted wedding the DJ was assigned to, and a random unknown UUID When the DJ calls getDjWedding with each Then each answers 404 NO_WEDDING with the same message, so the three cases cannot be told apart.
- [ ] #6 Given a member holding only the PLANNER role who fills no slot of wedding A When the planner calls getDjWedding for A Then the answer is 404 NO_WEDDING (BR-MX-42, BR-ID-23).
- [ ] #7 Given a member holding only the ADMIN role who fills no slot of wedding A of the business When the admin calls getDjWedding for A Then the answer is 200 with the same Wedding detail (deviation UD-14.b3; rekord-api answers 404 NO_WEDDING).
- [ ] #8 Given a couple portal session of wedding A, a second live wedding B of the same business, a wedding of another business and an unknown id When the session calls getDjWedding with each and calls listDjWeddings Then the answers are 403 FORBIDDEN for A and B, 404 NO_WEDDING for the other business's wedding and the unknown id, and 403 FORBIDDEN for listDjWeddings, because the wedding is looked up in the session's business before the missing account is refused (BR-CP-26; WeddingRepository.java:110-123, AppIdentity.java:81-87).
- [ ] #9 Given an assigned wedding that the planner created with couple display name "Emma & Julian", whose couple then called updatePortalCouple with names "Emma and Julian" When the DJ calls getDjWedding and listDjWeddings Then both carry couple_display_name "Emma & Julian" (deviation UD-18.b; rekord-api shows the couple's names, PortalResource.java:82).
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
