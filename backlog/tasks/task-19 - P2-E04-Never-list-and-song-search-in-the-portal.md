---
id: TASK-19
title: P2-E04 Never list and song search in the portal
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - epic
  - P2
milestone: m-2
dependencies:
  - TASK-18
references:
  - rekord-api/src/main/java/app/rekord/spotify/SpotifySearch.java
  - rekord-api/src/main/java/app/rekord/portal/PortalResource.java
  - rekord-contract/paths/portal.yaml
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:486-512'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 20400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to search songs and keep a never list so that the DJ never plays a song we banned.

Goal: the never list the exports filter on and the search the couple picks songs with. Scope: the operations putPortalBlocklistEntry, deletePortalBlocklistEntry (test-first, UD-15.d) and searchSongs; the Spotify search gateway tested against WireMock, with the contract's minLength 2 enforced by validation before the gateway is called (PIN-13-0492). Out of scope: applying the never list at export (P3-E07).

Plan item `P2-E04` (epic,P2) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given an open portal session When a partner of the couple calls searchSongs with q=a Then the answer is 422 VALIDATION_FAILED because the contract sets minLength 2, whether or not search credentials are configured.
- [ ] #2 Given an open portal session When a partner of the couple calls putPortalBlocklistEntry and then deletePortalBlocklistEntry for the same uid Then getPortalState lists the entry after the first call and not after the second.
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
