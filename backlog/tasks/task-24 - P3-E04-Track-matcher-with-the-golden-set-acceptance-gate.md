---
id: TASK-24
title: P3-E04 Track matcher with the golden-set acceptance gate
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - epic
  - P3
milestone: m-3
dependencies:
  - TASK-23
references:
  - rekord-api/src/main/java/app/rekord/matcher/Matcher.java
  - rekord-api/src/main/java/app/rekord/matcher/Score.java
  - rekord-api/src/main/java/app/rekord/matcher/Normalize.java
  - rekord-api/src/main/java/app/rekord/library/LibraryIndexCache.java
  - rekord-api/src/test/resources/golden-set.json
  - docs/rewrite/analysis/14-dj-matching-exports.md
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
priority: high
ordinal: 30400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want each requested song matched to the files of my active library with an auto, ambiguous or unmatched verdict so that I build a set without searching by hand.

Goal: the matcher, ported from rekord-api, accepted by the golden set. Scope: the operation matchTracks; normalisation, version extraction, signatures, the candidate gate, the fuzzy measures, the facets, the score and the buckets (BR-MX-01 to BR-MX-21); the playlist nudge, the playlist scope, the index cache and its invalidation (BR-LIB-42 to BR-LIB-46); the checked-in golden-set fixtures as the final acceptance gate with no generator (UX-07); the index not invalidated at scan completion (RISK-25); the unchecked playlist_id and the unbounded request (RISK-49). Three user decisions change rekord-api's matcher: auto also needs the best candidate's normalised artist and normalised core title to equal the requested song's, which turns 18 recorded auto cases of the golden set into ambiguous (deviation UD-19.c); normalisation keeps the letters and digits of every script (deviation UD-19.m6); and a request holds at most 1000 tracks, a contract change (deviation UD-19.m5). The golden fixtures keep their titles and artists, and the one file path in them becomes a synthetic /music/ path (UD-19.m3, RISK-15). Out of scope: remembered choices (P3-E05).

Plan item `P3-E04` (epic,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the golden library and the 212 golden-set cases checked into the repository When the matcher test runs in CI Then every case yields the bucket, the candidate order and the scores the checked-in golden set records, the buckets total 29 auto, 180 ambiguous and 3 unmatched, and the thresholds block equals the matcher's constants (UD-16.UX-07; deviation UD-19.c; rekord-api answers 47 auto and 162 ambiguous).
- [ ] #2 Given an active library while a scan of it runs When the DJ calls matchTracks Then the answer is 409 SCAN_IN_PROGRESS, and for an active library without tracks it is 409 NO_LIBRARY.
- [ ] #3 Given a library whose index a match has built When a scan of that library completes and the DJ calls matchTracks Then the candidates include the tracks the scan added (fixed defect RISK-25).
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
