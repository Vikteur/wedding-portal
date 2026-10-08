---
id: TASK-24.2
title: >-
  P3-E04-T02 Candidate retrieval: the fuzzy measures, the token gate, the
  fallback and library order
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 07:16'
updated_date: '2026-10-07 16:23'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-24.1
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/Fuzz.java:39-145'
  - 'rekord-api/src/main/java/app/rekord/matcher/LibraryIndex.java:24-195'
  - 'rekord-api/src/main/java/app/rekord/matcher/Matcher.java:44-70'
  - 'rekord-api/src/main/java/app/rekord/library/LibraryRepository.java:339-350'
  - 'rekord-api/src/test/java/app/rekord/matcher/FuzzTest.java:20-101'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:112-125'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:370-374'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:386-388'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: feature
ordinal: 30402
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want the matcher to look only at files that share words with the requested song, and at the closest titles when none do, so that a set of hundreds of songs is matched against twenty thousand files in seconds.

The port of Fuzz (BR-MX-10) and LibraryIndex (BR-MX-06 to BR-MX-09). A library file is indexed with its artist plus featured artists, its core title and an all-text string that includes the remixer; a query offers core, artist and remixer tokens, but its all-text string leaves the remixer out (BR-MX-07). Files are numbered in the order the database returns them for the library, sorted by path, and that number breaks every tie, so the tie order is the database's order. The rekord-api tests build the index from JSON and never meet the database's collation; this ticket asserts the order against the database itself and, as rekord-api does, pins no collation on the path (LibraryRepository.java:340-350), so the tie order is the order of the database's own collation. The fuzzy fallback breaks ties by that number ascending as rekord-api does; Python's tie order is undocumented and Python is retired (UD-8).

- Covers: BR-MX-06, BR-MX-07, BR-MX-08, BR-MX-09, BR-MX-10, PIN-14-0387, PIN-14-0513, PIN-14-0373, PIN-14-0512

Plan item `P3-E04-T02` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given rekord-api's fuzz-fixture.json with its 1,016 recorded pairs copied unchanged into wedding-portal's test resources When wedding-portal computes ratio, token_sort_ratio and token_set_ratio for every pair Then each value is within 1e-9 of the recorded one (BR-MX-10; FuzzTest.java:28-100).
- [x] #2 Given the pairs ("", ""), ("abc", "") and ("anthem", "other anthem of ours") When wedding-portal computes the fuzzy measures Then ratio("", "") is 100, ratio("abc", "") is 0, token_set_ratio("", "abc") is 0, token_set_ratio("anthem", "other anthem of ours") is 100 and token_sort_ratio of that pair is 46.15384615384615 within 1e-9 (BR-MX-10; Fuzz.java:39-145).
- [x] #3 Given a library file with artist "Artist One" and title "Anthem (feat. Guest Two) (Someone Remix)" and a file with no artist and title "Artist One - Anthem" When wedding-portal indexes them Then the first has artist text "artist one guest two", core "anthem", all-text "artist one guest two anthem someone" and 6 tokens, and the second has artist text null, core "artist one anthem" and all-text "artist one anthem" (BR-MX-06; LibraryIndex.java:72-108).
- [x] #4 Given a query with artist "Artist One" and title "Anthem (feat. Guest Two) (Someone Remix)" When wedding-portal builds its tokens and all-text Then the tokens are anthem, artist, guest, one, someone and two, and the all-text is "artist one guest two anthem" without the remixer (BR-MX-07; Matcher.java:48-70, LibraryIndex.java:175-186).
- [x] #5 Given a library of 51 files tagged "Band" / "Love" followed by one file z tagged "Solo" / "Love" When the DJ's query is "Solo" / "Love" and then "Band" / "Love" Then the first query has z as its only candidate, because "solo" is in at most 50 files, and the second lists 8 Band files, the last 8 in library order with the last first, and not z, because z shares only "love", which is in 52 files (BR-MX-08; LibraryIndex.java:110-148).
- [x] #6 Given a library of 400 files tagged "Band" / "Love" When wedding-portal asks the index for the candidates of the tokens band and love Then it returns 300 files, the last 300 in library order, starting with the last file (BR-MX-08; LibraryIndex.java:27-29).
- [x] #7 Given a library of two files a and b, in that library order, with no artist and the title "Dancing Queen" When the DJ's query has an empty artist, the title "Dancin Quen" and no duration Then no file passes the token gate, the fallback lists a then b, each with score 0.9314 and parts combined 0.9166666666666665, version 1.0 and duration null, and the bucket is ambiguous (BR-MX-09, PIN-14-0387, PIN-14-0513; LibraryIndex.java:151-172).
- [x] #8 Given a library with one file tagged "Daft Punk" / "One More Time" When the DJ's query has an empty artist and an empty title Then the fallback returns nothing, the candidates are empty and the bucket is unmatched (BR-MX-09; LibraryIndex.java:151-155).
- [x] #9 Given an active library in the test PostgreSQL holding 4 files tagged "Band" / "Love" at the paths "/m/b.mp3", "/m/B.mp3", "/m/a-b.mp3" and "/m/ab.mp3" When the test reads the library's tracks ordered by path from that database and then matches the query "Band" / "Love" Then the index numbers the files in the order the database returned, and the 4 candidates, all scored 1.0, are listed in the reverse of that order, whatever the database collation is (PIN-14-0373, PIN-14-0512; LibraryRepository.java:340-350).
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Every acceptance criterion of the ticket is checked by at least one automated test in wedding-portal that fails when the criterion is broken.
- [x] #2 The CI run of the ticket's pull request is green, read by commit SHA; no test, gate or check is skipped, disabled or quarantined to get there.
- [x] #3 The ArchUnit rules pass with no new entry in the frozen baseline.
- [x] #4 Every contract change the ticket needs is merged and tagged in rekord-contract before the code that uses it; wedding-portal pins that tag; no generated file is edited by hand.
- [x] #5 Every STOP item the ticket touches (contract push, database migration, authentication or the access matrix, encryption or logging of personal data, deleting history, production configuration or secrets) has a human approval recorded in the pull request before the step is taken.
- [x] #6 Every refusal is answered with the error envelope {"detail":{"code","message"}} and a code from the contract's ErrorCode enum; validation follows the Notification pattern (UD-12), and a 422 lists its violations in the `errors` list (UD-19.d).
- [x] #7 Every operation the ticket builds answers with the status, error code and body shape rekord-api gives for the same request, except for the deviations recorded as user decisions (UD-9 to UD-20) in docs/rewrite/STATUS.md; each deviation the ticket implements is named in the pull request.
- [x] #8 No secret, token, password, access code or personal data appears in logs, test data, commits or documents.
- [x] #9 Every schema change is a new Flyway migration (an applied migration is never edited) and is exercised by a Testcontainers test.
- [x] #10 Every document or code-map leaf that the change makes outdated is updated in the same pull request.
- [x] #11 The pull request is reviewed and merged into main.
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Close-out evidence (PR https://github.com/Vikteur/wedding-portal/pull/14, merged by Vikteur as e2c5e7a26e1983cc0dbf19cabe1dada16f6d19e5; CI run 37649743805 on head 667554dfbed6af26be1b3dafe81f34db4b5aa88c, jobs build and image both success; the build job runs `./gradlew test integrationTest build`, which includes check, integrationTest, resourceTest and startupTest).
- AC #1, #2: rekord-domain FuzzTest (the fixture is copied byte for byte from rekord-api, and the count of 1016 pairs is asserted; edge_cases).
- AC #3: LibraryIndexTest a_tagged_file_indexes_artist_plus_featured_core_and_remixer, a_file_without_artist_keeps_the_dash_in_its_core. AC #4: QueryTextTest.
- AC #5: LibraryIndexTest a_rare_token_alone_admits_a_file and two_shared_tokens_admit_a_file_but_one_common_token_does_not; TrackMatcherTest a_rare_token_match_lists_only_that_file and a_common_query_lists_the_last_8_band_files_last_first; the 50/51 rare boundary test.
- AC #6: LibraryIndexTest candidates_are_capped_at_300_last_in_library_order_first.
- AC #7: LibraryIndexTest with_no_shared_token_the_fallback_lists_equal_scores_in_library_order; TrackMatcherTest the_fallback_candidates_are_scored_on_the_combined_facet.
- AC #8: LibraryIndexTest the_fallback_returns_nothing_for_an_empty_query; TrackMatcherTest an_empty_query_is_unmatched.
- AC #9: application LibraryPathOrderIT (integrationTest, Testcontainers PostgreSQL 17). It runs rekord-api's libraryTracks query with no COLLATE against a C database and an ICU en-US database, and asserts that the two orders differ, that the index numbering follows each database's order, and that the 4 tied candidates (all 1.0) come back in reverse order.
- Each step was committed tests first, with its red run kept; .archon/scripts/tdd-check.sh passed. In review, five mutations of the code survived the tests; tests were added for them, and each mutation now fails at least one test. Review fix: LibraryIndex.items() and each file's token set are read-only (a failing test came first).
- DoD #1: see the AC list above. DoD #2: CI run 37649743805 green on 667554d; no test, gate or check is skipped or disabled.
- DoD #3 N/A: no ArchUnit suite or frozen baseline exists yet (rules A1-A13 arrive with TASK-3.1; docs/code-maps/archunit-fitness.md). The new code is in app.rekord.domain.matching, and its main classpath depends only on java.. (jackson-databind is test-only).
- DoD #4 N/A: no contract change; the pin stays at v0.1.0.
- DoD #5: the ticket touches no STOP item. AC #9 uses test DDL in a throwaway container, with no Flyway migration. The human review is the user's merge of PR #14 as e2c5e7a (UD-21.e).
- DoD #6 N/A: no operation and no refusal are built; the change is domain code only.
- DoD #7 N/A: no operation is built. The only behaviour that differs from rekord-api is the UD-8 tie order, which rekord-api shares (ordinal ascending).
- DoD #8: the test data is song titles, artists and file paths, with no personal data and no secrets.
- DoD #9 N/A: no schema change.
- DoD #10: docs/memory.md has the TASK-24.2 section (present at e2c5e7a). No code-map leaf became outdated.
- DoD #11: PR #14 was reviewed and merged by the user as e2c5e7a (UD-21.e).
- For the following tickets: TASK-24.3 adds the playlist nudge, duration_delta_sec and the UD-19.c auto rule to TrackMatcher/Score, and must replace the rekord-api three-guard bucket ported here. The tracks repository of TASK-22.1/TASK-24.5 must keep `order by t.path` without a COLLATE. Half-even rounding is untested against half-up, because no input was found where the two differ.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Ported rekord-api's Fuzz (ratio, token_sort_ratio, token_set_ratio) and LibraryIndex (file and query indexing, the token gate with the rare-token rule and the 300 cap, and the fuzzy fallback with ordinal-ascending ties) into app.rekord.domain.matching, plus the part of Score/matchOne that the criteria reach (TrackMatcher). The index is read-only. Library order is the database's own order for `order by t.path`, verified under the C and ICU collations. Verified by FuzzTest (1016 rapidfuzz pairs), LibraryIndexTest, QueryTextTest, TrackMatcherTest and LibraryPathOrderIT; PR #14 merged as e2c5e7a26e1983cc0dbf19cabe1dada16f6d19e5; CI run 37649743805 green on head 667554dfbed6af26be1b3dafe81f34db4b5aa88c.
<!-- SECTION:FINAL_SUMMARY:END -->
