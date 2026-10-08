---
id: TASK-20
title: P2-E05 Portal access matrix and change rows across every portal operation
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - epic
  - P2
milestone: m-2
dependencies:
  - TASK-19
references:
  - 'docs/rewrite/analysis/12-couple-portal.md:74'
  - 'docs/rewrite/analysis/12-couple-portal.md:86-87'
  - 'docs/rewrite/analysis/12-couple-portal.md:96'
  - 'docs/rewrite/analysis/12-couple-portal.md:650-689'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 20500
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want every portal route to answer only to our session and every change we make to be recorded so that our wishes stay ours and the DJ sees what changed.

Goal: the rules that hold across all portal operations once each is built. Scope: the access matrix of the portal (both credentials on every call, the friends' narrow scope, the couple-only operations, only the rm_portal cookie counting on portal routes and only the rm_session cookie on account routes, UD-19.l3), one song_changes row per portal write, and the PIN(test) cases and untested behaviours of slice 12 as named tests. Out of scope: the DJ's change feed that reads these rows (P3-E01) and the frontends.

Plan item `P2-E05` (epic,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a FRIENDS session of a wedding When it calls each of the 7 couple-only portal operations Then updatePortalCouple, putPortalBlocklistEntry, getPortalTimeline and getPortalTasks answer 403 FORBIDDEN "That part belongs to the couple.", and deletePortalEntry, reorderPortalList and deletePortalBlocklistEntry answer 403 FORBIDDEN "Friends can add songs but cannot change or remove them." (deviation UD-18.a/UD-19.l1 for the last three; rekord-api answers them 403 FORBIDDEN "That part belongs to the couple.").
- [ ] #2 Given a COUPLE session and a FRIENDS session of a wedding When each makes a portal write that answers 200 Then exactly one song_changes row is added for each write, and a refused write adds none.
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
