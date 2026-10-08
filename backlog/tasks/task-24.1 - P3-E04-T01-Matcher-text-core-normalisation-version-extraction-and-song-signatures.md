---
id: TASK-24.1
title: >-
  P3-E04-T01 Matcher text core: normalisation, version extraction and song
  signatures
status: Done
assignee:
  - '@claude'
created_date: '2026-10-07 07:16'
updated_date: '2026-10-07 14:10'
labels:
  - user-story
  - P3
milestone: m-3
dependencies: []
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/Normalize.java:25-85'
  - 'rekord-api/src/main/java/app/rekord/matcher/Versions.java:22-235'
  - 'rekord-api/src/main/java/app/rekord/matcher/Signature.java:30-48'
  - 'rekord-backend/tests/test_normalize.py:1-35'
  - 'rekord-backend/tests/test_versions.py:1-60'
  - 'rekord-backend/tests/test_preferences.py:31-54'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:102-111'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:379-385'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: feature
ordinal: 30401
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want two spellings of one song read as the same song and two versions of it read as different songs so that the matcher compares songs instead of spellings.

Pure functions ported from rekord-api's matcher package with unit tests and no database: Normalize (BR-MX-01), Versions with its phrase table and the empty-core rule (BR-MX-02 to BR-MX-04) and Signature (BR-MX-05). The signature id names remembered choices (P3-E05), so every id below must come out exactly. rekord-backend is retired (UD-8), so the comparisons the analysis asks for between the two normalisers become fixed tables of rekord-api's outputs. One change, decided by the user: the last step of Normalize keeps every letter (Unicode category L) and every decimal digit (Unicode category Nd) of every script and turns each run of other characters into one space, where rekord-api keeps only a-z and 0-9, so Cyrillic, Greek and CJK titles no longer normalise to the empty string and no longer share one signature id (deviation UD-19.m6, PIN-14-0383, PIN-14-0514). The steps before it stay rekord-api's, in rekord-api's order: NFKD, combining marks removed, the fixed transliteration table (ø to o, æ to ae, œ to oe, ß to ss, đ and ð to d, ł to l, þ to th, upper case alike), lower case under the root locale, & to " and ", a + between two ASCII word characters to " and ", $ to s, and apostrophes removed (Normalize.java:50-69).

- Covers: BR-MX-01, BR-MX-02, BR-MX-03, BR-MX-04, BR-MX-05, PIN-14-0383, PIN-14-0514, UD-19.m6

Plan item `P3-E04-T01` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Given the inputs "Étienne de Crécy", "Møme & RØMANS", "KE$HA", "Don't Stop Believin'", "Kungs & Cookin’ On 3 Burners" and "A+B" When wedding-portal normalises them Then the outputs are "etienne de crecy", "mome and romans", "kesha", "dont stop believin", "kungs and cookin on 3 burners" and "a and b" (BR-MX-01; test_normalize.py:7-19).
- [x] #2 Given the inputs "Señorita (ft. Camila)", "  spaced   out  ", "Cœur de pirate", "Blitzkrieg ß", "Łódź Þór Đorđe", the empty string, null, and "Crécy" once with é as one code point and once as e plus a combining accent When wedding-portal normalises them Then the outputs are "senorita ft camila", "spaced out", "coeur de pirate", "blitzkrieg ss", "lodz thor dorde", "", "", "crecy" and "crecy" (BR-MX-01; test_normalize.py:7-35).
- [x] #3 Given the inputs "The XX - Intro!", "" and "..." When wedding-portal tokenises them Then the token lists are [the, xx, intro], [] and [] (BR-MX-01; test_normalize.py:26-29).
- [x] #4 Given the inputs "Кино", "Мумий Тролль", "Άλφα Βήτα", "東京事変", "ABBA Ёлка", "ＡＢＢＡ" and "Кино+Ария" When wedding-portal normalises them and computes the signature of ("Кино", "Группа крови") Then the outputs are "кино", "мумии тролль", "αλφα βητα", "東京事変", "abba елка", "abba" and "кино ария", and the signature is кино|группа крови|| with signature id b13c96a2b1b93681 (deviation UD-19.m6; rekord-api answers "", "", "", "", "abba", "abba" and "", and the id 98c4b7d37a4c63c3 of an empty artist and title; Normalize.java:50-69).
- [x] #5 Given the titles "Am I Wrong", "Am I Wrong (Original Mix)", "Substitution (Purple Disco Machine Remix)", "Am I Wrong [Superdiscount Extended Edit]", "Animals - Radio Edit", "Animals (Radio Edit)", "Levels (Remastered 2011)" and "Divide - Part 2" When wedding-portal extracts the version Then core, descriptors and remixer are (Am I Wrong, [], null), (Am I Wrong, [], null), (Substitution, [remix], purple disco machine), (Am I Wrong, [extended], superdiscount), (Animals, [radio], null) twice, (Levels, [remaster], null) and (Divide - Part 2, [], null) (BR-MX-02, BR-MX-03; test_versions.py:9-17).
- [x] #6 Given the titles "(I Can't Get No) Satisfaction", "One (Club Mix)", "Song (VIP)", "Song (Acoustic)", "Song (Sped Up)", "Faded (Restrung)", "Greyhound - Extended Mix" and "Titel - Live - Radio Edit" When wedding-portal extracts the version Then core and descriptors are ((I Can't Get No) Satisfaction, []), (One, [club]), (Song, [vip]), (Song, [acoustic]), (Song, [spedup]), (Faded (Restrung), []), (Greyhound, [extended]) and (Titel, [live, radio]), each with remixer null (BR-MX-02, BR-MX-03; test_versions.py:18-25).
- [x] #7 Given the titles "Song (Rmx)", "Song (A Cappella)", "Song (Slowed and Reverb)", "Song {Dub}", "Song (A B C D Remix)", "Song (A B C D E Remix)" and "Extended Mix" When wedding-portal extracts the version Then the results are (Song, [remix], null), (Song, [acapella], null), (Song, [slowed], null), (Song, [dub], null), (Song, [remix], a b c d), (Song (A B C D E Remix), [], null) because 5 leftover words exceed the limit of 4, and (Extended Mix, [], null) because a title of descriptors only keeps its source as core (BR-MX-03, BR-MX-04; Versions.java:93-171).
- [x] #8 Given the titles "Peaches (feat. Daniel Caesar & Giveon)", "One More Time (feat. Someone) [Club Mix]", "Song (with Dua Lipa)", "I'm the One feat. DJ Khaled" and "Solo ft Demi Lovato" When wedding-portal extracts the version Then core and featured are (Peaches, [daniel caesar and giveon]), (One More Time, [someone]) with descriptors [club], (Song, [dua lipa]), (I'm the One, [dj khaled]) and (Solo, [demi lovato]) (BR-MX-02; test_versions.py:36-48).
- [x] #9 Given the pairs ("Artist One", "Anthem"), ("artist one", "ANTHEM!"), ("Justin Bieber", "Peaches (feat. Daniel Caesar)"), ("Justin Bieber", "Peaches"), (null, "Anthem") and ("Artist Two", "Anthem") When wedding-portal computes signature and signature id Then they are artist one|anthem|| with 5640fae95ee899f8 twice, justin bieber|peaches|| with 79a721bf6e0b7f26 twice because featured artists are left out, |anthem|| with 7298f481ab00c4b9, and artist two|anthem|| with 077c6b511f37749e (BR-MX-05; Signature.java:30-48).
- [x] #10 Given the pairs ("deadmau5", "Strobe"), ("deadmau5", "Strobe (Radio Edit)"), ("deadmau5", "Strobe (Someone Remix)"), ("deadmau5", "Strobe (Original Mix)") and ("Daft Punk", "One More Time") When wedding-portal computes the signature id, the first 16 hex characters of SHA-1 over the UTF-8 signature Then the ids are 46b0b3916e07bdf4, 4a6e7911f10c5a0b, 9126c0757348a076 for the signature deadmau5|strobe|remix|someone, 46b0b3916e07bdf4 again, and 4cad791cad0c0451 (BR-MX-05, PIN-14-0515; test_preferences.py:43-50).
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
Finalization evidence (2026-10-07): PR https://github.com/Vikteur/wedding-portal/pull/9 merged by the user as 3187e9cad9ccbb95aef7cbf4cd705f59fdbe95f3 (UD-21.e); head 8ed4b4a2530824b0afae32de10279627a1c6e4fb, tree identical to the merge commit. CI run 37633679789 (workflow CI, headSha 8ed4b4a) concluded success: jobs build and image green; './gradlew test integrationTest build' BUILD SUCCESSFUL, 42 actionable tasks: 42 executed, :rekord-domain:test executed; 'Upload test reports' was skipped only because its condition is failure(); 'Contract pin is a tag' OK.
AC evidence (rekord-domain/src/test/java/app/rekord/domain/matching, exact-value assertions): AC1 NormalizeTest.normalises_latin_spellings_to_one_ascii_form; AC2 folds_brackets_spacing_ligatures_and_special_letters, normalises_null_to_the_empty_string, composed_and_decomposed_accents_normalise_alike; AC3 tokenises_on_the_normalised_spaces; AC4 keeps_letters_and_digits_of_every_script + SignatureTest.a_cyrillic_song_gets_its_own_signature_id (b13c96a2b1b93681, not 98c4b7d37a4c63c3) + an_empty_artist_and_title_give_the_empty_signature; AC5 VersionsTest.strips_version_brackets_and_suffixes_into_descriptors; AC6 keeps_non_version_brackets_and_canonicalises_synonyms; AC7 applies_the_leftover_limit_and_the_empty_core_rule; AC8 reads_featured_artists_from_segments_and_inline_tails; AC9 SignatureTest.two_spellings_share_a_signature_and_featured_artists_are_left_out; AC10 versions_and_remixers_give_their_own_ids + a_remix_signature_names_the_descriptor_and_the_remixer. Local rerun of :rekord-domain:test at 8ed4b4a: NormalizeTest 25, SignatureTest 14, VersionsTest 32 tests, 0 skipped, 0 failures.
DoD #1: every AC has the test(s) above asserting exact outputs. DoD #2: CI run 37633679789 green on 8ed4b4a, nothing skipped/disabled (no @Disabled, no exclusions added). DoD #3 N/A: no ArchUnit suite or frozen baseline exists in wedding-portal yet (docs/code-maps/archunit-fitness.md; rules arrive with TASK-3.1, To Do); the PR adds no baseline file and keeps rekord-domain's main classpath free of libraries (test-only JUnit/AssertJ in rekord-domain/build.gradle.kts). DoD #4 N/A: pure domain functions, no contract change, no generated file touched; contract pin still a tag. DoD #5: no STOP item touched (PR body 'Approval'); the user's merge of PR #9 as 3187e9c (UD-21.e) is the recorded human approval. DoD #6 N/A: no operation or refusal is built; pure functions only. DoD #7 N/A: no operation is built; the one deviation, UD-19.m6, is implemented and named in the PR body and in docs/memory.md. DoD #8: diff holds only public song titles/artist names as test data, no secrets or personal data. DoD #9 N/A: no schema change, no migration. DoD #10: docs/memory.md updated with the matcher-core decisions; code-map leaves java.md and jvm-testing.md remain accurate. DoD #11: reviewed and merged by the user as 3187e9c (UD-21.e).
Follow-up (not created): TASK-3.1 should add the ArchUnit suite so A1 covers app.rekord.domain.matching.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Ported rekord-api's matcher text core into rekord-domain (app.rekord.domain.matching): Normalize (keeps Unicode letters and Nd digits per UD-19.m6), Versions (core, descriptors, remixer, featured, leftover limit and empty-core rule) and Signature (norm(artist)|norm(core)|descriptors|norm(remixer), id = first 16 hex of SHA-1). Verified by NormalizeTest, VersionsTest and SignatureTest (71 tests, every AC value asserted exactly) in CI run 37633679789 on head 8ed4b4a2530824b0afae32de10279627a1c6e4fb (green); PR https://github.com/Vikteur/wedding-portal/pull/9 reviewed and merged by the user as 3187e9cad9ccbb95aef7cbf4cd705f59fdbe95f3.
<!-- SECTION:FINAL_SUMMARY:END -->
