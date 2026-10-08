---
id: TASK-21.1
title: 'P3-E01-T01 Build listDjWeddings: the weddings a DJ is on, by team assignment'
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - user-story
  - P3
  - stop-auth-access
milestone: m-3
dependencies:
  - TASK-20
  - TASK-11
references:
  - 'rekord-api/src/main/java/app/rekord/portal/CouplesResource.java:63-70'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:45-83'
  - 'rekord-contract/paths/dj.yaml:15-32'
  - 'rekord-contract/components/planner.yaml:364-410'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:177'
  - 'docs/rewrite/STATUS.md:301'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-21
priority: high
type: feature
ordinal: 30101
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to list the weddings I am booked for so that I prepare the music of exactly those weddings.

The DJ app opens on this list (rekord-dj/src/api.ts:190). rekord-api shows a wedding through the DJ door only when the signed-in account fills one of its team slots in state PENCILLED or CONFIRMED, never because of the role alone (WeddingRepository.assignedTo and visibleTo); a planner asking through this door used to see every couple's songs, which is why the assignment rule was added. UD-14.b3 lets an admin see every wedding of the business here without a slot. Each row is the WeddingSummary that rekord-api builds from the wedding, its team and its song lists. The team itself is assigned by P1-E05.

- Builds: `listDjWeddings`
- STOP (human approval in the pull request): auth-access
- Error code `FORBIDDEN` without a criterion here: answered by the account check of the use case with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} to a session that carries no member account, such as a couple portal session, as rekord-api answers (AppIdentity.java:81-87, ErrorMappers.java:67-73); asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01

Plan item `P3-E01-T01` (user-story,P3,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a DJ who fills a team slot of wedding A in state PENCILLED and of wedding B in state CONFIRMED, and fills no slot of wedding C, all three weddings of the DJ's business and not deleted When the DJ calls listDjWeddings Then the answer is 200 with a WeddingList holding A and B ordered by wedding_date descending, and C is not in it.
- [ ] #2 Given a DJ whose CONFIRMED team slot is on a wedding that is soft-deleted (deleted_at set) When the DJ calls listDjWeddings Then that wedding is not in the list.
- [ ] #3 Given a member signed in to business X who fills a CONFIRMED slot of a wedding of business X and also fills a CONFIRMED DJ slot of a wedding of business Y When the member calls listDjWeddings Then only the wedding of business X is listed.
- [ ] #4 Given a member holding only the PLANNER role who fills no team slot, in a business with 3 weddings When the planner calls listDjWeddings Then the answer is 200 with an empty weddings list (BR-MX-42).
- [ ] #5 Given a member holding only the ADMIN role who fills no team slot, in a business with 3 weddings that are not deleted and 1 soft-deleted wedding When the admin calls listDjWeddings Then the answer is 200 with the 3 weddings that are not deleted, ordered by wedding_date descending (deviation UD-14.b3; rekord-api lists only weddings the account is assigned to).
- [ ] #6 Given an assigned wedding with a team and song lists When the DJ calls listDjWeddings Then its row carries id, slug, status, wedding_date, days_until, couple_display_name, guest_count, note, venue, roles, and music with lists_in and lists_total, each filled from the wedding, its team and its song lists (WeddingMapper.java:55-70, CouplesResource.java:63-70), and its music field carries counts and no song titles.
- [ ] #7 Given an assigned wedding whose couple_top20 list holds 3 entries, must_plays 1, never 2 and playlist_links 4, and whose opening_dance, second_third and friends_top20 lists are empty When the DJ calls listDjWeddings Then its music field has lists_in 2 and lists_total 5, because the never list and playlist_links do not count toward progress (deviation UD-18.c; rekord-api answers lists_in 3 and lists_total 6, WeddingMapper.java:181-199, R__seed_song_list_kinds.sql).
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
