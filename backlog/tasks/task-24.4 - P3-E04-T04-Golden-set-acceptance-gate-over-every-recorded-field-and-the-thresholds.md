---
id: TASK-24.4
title: >-
  P3-E04-T04 Golden-set acceptance gate over every recorded field and the
  thresholds
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - technical
  - P3
milestone: m-3
dependencies:
  - TASK-24.3
references:
  - 'rekord-api/src/test/resources/golden-set.json:1-30'
  - 'rekord-api/src/test/java/app/rekord/matcher/MatcherGoldenSetTest.java:40-196'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:366-372'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:425'
  - 'docs/rewrite/STATUS.md:339-340'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: task
ordinal: 30404
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a developer of wedding-portal, I want the matcher run against rekord-api's golden set on every build so that a change that moves one bucket, pick, score or facet fails the build.

UD-16 makes the checked-in golden fixtures final, with no generator, and the matcher's acceptance gate (UX-07). The gate copies golden-set.json and golden-library.json from rekord-api's test resources with two changes. First, the fixtures come from a real music library: the one file path in them, source.database of golden-set.json, which names a database file on the recording computer, becomes "/music/golden-src.db", while every title, artist, track id and duration stays; golden-library.json holds no path (UD-19.m3, RISK-15). Second, the 18 cases that rekord-api answers auto with a best candidate whose normalised artist or normalised core title differs from the query's, or for a query without an artist, expect ambiguous with auto_selected_id null and their recorded candidates, and the summary block counts them so (deviation UD-19.c). The normalisation of UD-19.m6 changes no recorded expectation: the 10 library tracks whose normalised text it changes are in no case's candidate list, and no query's normalised text changes. The gate builds the index from the library file in its order with its 9 preferences and 13 membership rows, and runs the 212 cases. It checks more than rekord-api's MatcherGoldenSetTest, which compares only ids and scores: it also compares facet parts, the delta, input_version, candidate versions and playlists, and the thresholds block (PIN-14-0515, PIN-20-0174). rekord-api's matcher classes reproduce every other recorded field within 1e-6.

- Covers: UX-07, UD-16.UX-07, PIN-14-0515, PIN-20-0174, RISK-15, UD-19.m3, UD-19.c, UD-19.m6

Plan item `P3-E04-T04` (technical,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given golden-set.json and golden-library.json copied from rekord-api's test resources with only the two changes of this ticket When the gate builds the index from the library file Then the index holds as many files as source.library_size of the set says, 20,869, the gate reads 212 cases, 9 preferences and 13 membership rows, source.database is "/music/golden-src.db", and no string in either fixture starts with a drive letter followed by ":/" or ":\", or with "/Users/" or "/home/" (UD-19.m3, RISK-15).
- [ ] #2 Given the 212 golden cases When the gate matches each query with the recorded preferences and membership Then every bucket, auto_selected_id, from_preference and candidate_count equals the checked-in one, the buckets total 29 auto, 180 ambiguous and 3 unmatched, and the cases at the 0-based positions 82, 85, 86, 88, 90, 100, 101, 107, 108, 109, 110, 111, 115, 122, 152, 155, 156 and 161 are ambiguous with auto_selected_id null, from_preference false and their recorded candidates (UX-07; deviation UD-19.c; rekord-api answers auto with a candidate for these 18 cases).
- [ ] #3 Given the 212 golden cases When the gate compares each candidate list Then the track ids are in the recorded order, every score and every non-null facet in parts is within 1e-6 of the recorded value, the set of facet names and the null facets are the same, duration_delta_sec, version descriptors, remixer and playlists are equal, and so is input_version (PIN-14-0369, PIN-14-0515).
- [ ] #4 Given the thresholds block of golden-set.json When the gate compares it with the matcher's constants Then all 14 keys equal them: report 0.45, strong 0.60, auto score 0.82, auto margin 0.10, auto minimum version 0.90, auto minimum duration 0.55, 8 candidates, the weights 0.40, 0.30, 0.70, 0.15 and 0.15, and the playlist nudge 0.02 capped at 3 (PIN-20-0174; Score.java:24-49).
- [ ] #5 Given the 9 cases of the family preference When the gate runs them Then each has from_preference true, the recorded auto_selected_id and the recorded bucket, and the 12 cases of the family playlist_member keep their recorded bucket (MatcherGoldenSetTest.java:162-195).
- [ ] #6 Given a copy of golden-set.json in which the test changes one recorded candidate score of one case by 0.001 When the gate runs against that copy Then it fails and its message names the case by its 0-based position in cases and its family, the candidate's track id, the expected score and the actual score.
- [ ] #7 Given the build of wedding-portal When the test task runs Then the golden gate runs in it without a database or network and fails the build on any difference.
- [ ] #8 Given the checked-in golden-set.json When the gate reads its summary block Then by_bucket is auto 29, ambiguous 180 and unmatched 3, by_family typo is auto 3 and ambiguous 22, no_artist is ambiguous 25, feat_inline is ambiguous 12, and every other family keeps the counts rekord-api's golden-set.json records (deviation UD-19.c; rekord-api records typo auto 8 and ambiguous 17, no_artist auto 9 and ambiguous 16, feat_inline auto 4 and ambiguous 8).
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
