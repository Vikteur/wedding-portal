---
id: TASK-10.3
title: >-
  P1-E04-T03 List the weddings of the business with the date filter and search
  (listWeddings)
status: To Do
assignee: []
created_date: '2026-10-07 07:11'
labels:
  - user-story
  - P1
  - stop-auth-access
milestone: m-1
dependencies:
  - TASK-10.2
references:
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingsResource.java:77-85'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingRepository.java:49-83'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:55-70'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingMapper.java:176-222'
  - 'rekord-api/src/main/java/app/rekord/wedding/WeddingService.java:250-263'
  - 'rekord-contract/paths/planner.yaml:9-40'
  - 'rekord-contract/components/planner.yaml:364-424'
  - 'docs/rewrite/analysis/11-planner-weddings.md:103-106'
  - 'docs/rewrite/analysis/11-planner-weddings.md:281'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-10
priority: high
type: feature
ordinal: 10403
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a planner, I want the overview of upcoming, past or all weddings with a search over couple, venue and note so that I find any wedding of the business in one place.

Builds the operation listWeddings. Oracle: WeddingsResource.listWeddings (WeddingsResource.java:77-85), WeddingRepository.visibleTo (WeddingRepository.java:49-83) and WeddingMapper.summary, teamTags and progress (WeddingMapper.java:55-70, :176-199, :212-222). status upcoming is a wedding date on or after today and a status other than CANCELLED; past is a date before today, cancelled ones included; all and any other value apply no date filter (BR-PL-24). Today is the date of the application Clock in UTC, as rekord-api reads LocalDate.now() in a JVM run with user.timezone UTC (WeddingRepository.java:66-69), not the date in the wedding's time zone; tests fix the Clock. An omitted status means upcoming, the contract default (PIN-11-0281, RISK-46): rekord-api's generated interface cannot be read in the tree, so the characterization test of this ticket records rekord-api's answer without status and the build follows the contract. q is a case-insensitive substring over couple name, venue name and note, with LIKE wildcards not escaped. The order is wedding date descending, without paging. Each summary carries the roles pills, one per team slot ordered by role and sort order with label set to the slot's person name only (BR-PL-27; vendor and contact names are never resolved, RISK-46), and music progress: kinds that do not count toward progress are skipped and a list is in when it holds a song (UD-18.c) (BR-PL-26, BR-CP-22). Visibility follows BR-PL-03 and every lookup is filtered on the caller's business (BR-ID-21).

- Builds: `listWeddings`
- STOP (human approval in the pull request): auth-access
- Covers: BR-PL-24, BR-PL-26, BR-PL-27, BR-PL-03, BR-CP-22, BR-ID-21, BR-DM-38, RISK-46, PIN-11-0281, PIN-11-0397
- Error code `FORBIDDEN` without a criterion here: answered 403 FORBIDDEN when the caller lacks the role (the role-denied envelope of P1-E09-T01) or the session carries no account, for instance a portal session (AppIdentity.java:73-86, ErrorMappers.java:67-73); asserted for this operation by the access matrix of P1-E09-T03.
- Error code `NOT_SIGNED_IN` without a criterion here: answered 401 NOT_SIGNED_IN "Sign in to continue." by the route guard of P0-E06 to a request without a live session, before the operation runs; asserted for this operation by the access matrix of P1-E09-T03.

Plan item `P1-E04-T03` (user-story,P1,stop-auth-access) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the application clock fixed at 2027-06-01T10:00Z and the weddings A dated 2027-06-12 CONFIRMED, B dated 2027-08-01 DRAFT, C dated 2027-05-01 COMPLETED, D dated 2027-07-01 CANCELLED and E dated 2027-07-15 with deleted_at set When a planner calls listWeddings with status upcoming, past and all Then the answers list B, A, then C, then B, D, A, C (BR-PL-24, BR-DM-38).
- [ ] #2 Given the same weddings When a planner calls listWeddings with no status, and with status "soon" Then the first answer lists B, A, as for upcoming, and the second lists B, D, A, C (PIN-11-0281, PIN-11-0397, RISK-46).
- [ ] #3 Given A named "Emma & Julian", B with venue name "De Oude Tuinderij" and C with note "Allergy list sent" When a planner calls listWeddings with status all and q "JULIAN", "tuinderij", "allergy" and "%" Then the answers list A, then B, then C, then every live wedding of the business (BR-PL-24).
- [ ] #4 Given wedding A with its DJ slot PENCILLED with person name "DJ Ray" and its PHOTO slot CONFIRMED with only a vendor When a planner calls listWeddings with status all Then A's summary carries id, slug, status, wedding_date, days_until 11, couple_display_name, venue, guest_count, note and the roles CATERING, DJ, MC, PHOTO and VENUE, the DJ pill with state PENCILLED and label "DJ Ray" and the PHOTO pill with state CONFIRMED and label null (BR-PL-27, RISK-46).
- [ ] #5 Given wedding A whose couple_top20 list holds 3 songs, whose must_plays list holds 1 song, whose never list holds 2 songs and whose playlist_links list holds 4 songs When a planner calls listWeddings Then A's music is lists_in 2 and lists_total 5 (BR-PL-26, BR-CP-22, deviation UD-18.c).
- [ ] #6 Given a DJ member named on A's DJ slot as CONFIRMED, on B's PHOTO slot as PENCILLED, on C's DJ slot as OPEN and on D's MC slot as NOT_NEEDED When the DJ calls listWeddings with status all Then the answer lists B and A only (BR-PL-03).
- [ ] #7 Given a wedding of another business dated 2027-06-12 When a planner of this business calls listWeddings with status all and with q "Emma" Then the other business's wedding is never listed (BR-ID-21).
- [ ] #8 Given rekord-api as the behaviour oracle When the characterization test calls listWeddings against rekord-api without status for a wedding dated yesterday and one dated tomorrow Then it records which of the two are listed, and the recorded answer is attached to this ticket before the build is accepted (PIN-11-0281).
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
