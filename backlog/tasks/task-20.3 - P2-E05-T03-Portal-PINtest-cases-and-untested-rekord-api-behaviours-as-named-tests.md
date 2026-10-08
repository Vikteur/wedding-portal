---
id: TASK-20.3
title: >-
  P2-E05-T03 Portal PIN(test) cases and untested rekord-api behaviours as named
  tests
status: To Do
assignee: []
created_date: '2026-10-07 07:15'
labels:
  - technical
  - P2
milestone: m-2
dependencies:
  - TASK-16.4
  - TASK-20.1
  - TASK-20.2
references:
  - 'docs/rewrite/analysis/12-couple-portal.md:189'
  - 'docs/rewrite/analysis/12-couple-portal.md:533'
  - 'docs/rewrite/analysis/12-couple-portal.md:599'
  - 'docs/rewrite/analysis/12-couple-portal.md:650-689'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-20
priority: medium
type: task
ordinal: 20503
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want every pinned portal case of slice 12 to run as a named test so that a later change cannot drift from the oracle unnoticed.

Slice 12 lists 11 PIN(test) cases (P1 to P11, docs/rewrite/analysis/12-couple-portal.md:650-689) and the behaviours rekord-api never tested (R21). The phase-2 tickets state each expected answer; this ticket makes sure each one runs as its own named test in CI, and records the framework answers that only rekord-api can show (P0-E05). The phase-2 build tickets state wedding-portal's own answers to the framework-error requests (UD-19.d3, UD-20.c).

- Covers: PIN-12-0189, PIN-12-0533, PIN-12-0599, PIN-12-0650

Plan item `P2-E05-T03` (technical,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the PIN(test) cases P1 to P11 of slice 12 When the phase-2 suite runs in CI Then P1, P2, P3, P4, P6, P7, P8 and P11 each pass as a named test with the outcome their ticket states (P1 and P3 in P2-E02-T01, P2 for the body {}, P7 and P11 in P2-E01-T01, P4 in P2-E04-T03, P6 in P2-E02-T02, P8 in P2-E03-T03; the rekord-api answers of P2 and P6 are recorded by criterion 3), P9 passes as a named test of this ticket in which a COUPLE session opened on 2027-06-10 for the wedding dated 2027-06-12 calls getPortalState and gets 200 at 23:59 on 2027-06-19 and 410 LINK_EXPIRED at 00:00 on 2027-06-20 (deviation UD-9.c), P5 is the test of P3-E01 and P10 is deferred with the frontends.
- [ ] #2 Given the untested behaviours of slice 12 R21 When the phase-2 suite runs in CI Then each has at least one named passing test: LINK_EXPIRED, lock escalation and reset, the 300 ms floor, the session lifetimes, reorder, UID_CONFLICT, revoke as seen by an open session, the details PATCH, and the timeline and tasks views, while the getDjWedding detail is pinned in P3-E01.
- [ ] #3 Given rekord-api running the framework-error requests When a body that is not JSON is sent to openPortalSession and to putPortalEntry, putPortalEntry carries kind "dance", putPortalEntry carries position "first", and updatePortalCouple carries wedding_date "2027-13-40" Then the status, detail.code and detail.message rekord-api answers to each of the 5 requests are recorded in a named characterization test against rekord-api (slice 12 P2 and P6), while wedding-portal answers them as P2-E01-T01 criterion 12, P2-E03-T01 criterion 12 and P2-E02-T02 criterion 5 state.
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
