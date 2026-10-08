---
id: TASK-18
title: 'P2-E03 Song wishes: entries and order of the song lists'
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - epic
  - P2
milestone: m-2
dependencies:
  - TASK-17
references:
  - rekord-api/src/main/java/app/rekord/portal/SongService.java
  - rekord-api/src/main/java/app/rekord/portal/PortalResource.java
  - rekord-contract/paths/portal.yaml
  - docs/rewrite/analysis/12-couple-portal.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 20300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to add, remove and reorder the songs on our lists so that the DJ plays the night we planned.

Goal: the song lists the DJ works from. Scope: the operations putPortalEntry, deletePortalEntry and reorderPortalList (reorderPortalList is built test-first, UD-15.d); friends only adding wishes through the friends' link (UD-18.a, UD-19.l1, UD-19.l2, RISK-17): every FRIENDS-session call that would change or remove an existing entry is refused with 403 FORBIDDEN "Friends can add songs but cannot change or remove them.", whoever saved the entry; that is a putPortalEntry for a uid already stored, and every deletePortalEntry, reorderPortalList and deletePortalBlocklistEntry (P2-E04-T02) call whatever uid or list it names; "This link can only add to the friends' top 20." stays only for a FRIENDS-session putPortalEntry whose kind is not friends_top20, which is checked first, so no refusal matches both texts; a FRIENDS-session repeat of a friends_top20 entry with identical fields answers 200 and changes nothing; writes that do not lock the list (RISK-37); lists never marked SUBMITTED or LOCKED and the never list left out of progress (UD-18.c, RISK-45); start_pref removed from the contract, with remarks for the DJ in note (UD-18.d). Out of scope: the retrying saver of the couple app (RISK-38) and the doubled link prefix (RISK-47), both frontend matters.

Plan item `P2-E03` (epic,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an open portal session and a list holding 3 entries When a partner of the couple calls putPortalEntry for a new uid, deletePortalEntry for one existing uid and reorderPortalList with the remaining uids reversed Then getPortalState lists the remaining 3 entries in the new order.
- [ ] #2 Given a list at its entry cap When two putPortalEntry calls for new uids arrive at the same time Then at most one is stored and the list never holds more entries than its cap (RISK-37).
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
