---
id: TASK-25
title: P3-E05 Remembered match choices
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-24
references:
  - rekord-api/src/main/java/app/rekord/matcher/PreferenceService.java
  - rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java
  - rekord-api/src/main/java/app/rekord/domain/Preference.java
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:509-511'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30500
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want the matcher to remember the file I picked for a song so that the same song is matched to my choice next time.

Goal: the preferences of a library. Scope: the operations listPreferences, rememberPreference, forgetAllPreferences and forgetPreference; a remembered choice winning in the matcher (BR-MX-18); remember as an upsert and the forget rules (BR-MX-22, BR-MX-23); the race of two first remember calls that answers 500 today; file_label read from the global tracks table (RISK-29). Out of scope: the matcher core (P3-E04).

Plan item `P3-E05` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a remembered choice for an artist and title When the DJ calls matchTracks for that artist and title Then the remembered file is the first candidate.
- [ ] #2 Given no preference for an artist and title When two rememberPreference calls for them arrive at the same time Then both answer 200 and one preference row exists (fixed defect; rekord-api answers 500 to one of them).
- [ ] #3 Given a preference id that does not exist in the active library When the DJ calls forgetPreference Then the answer is 404 NO_PREFERENCE.
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
