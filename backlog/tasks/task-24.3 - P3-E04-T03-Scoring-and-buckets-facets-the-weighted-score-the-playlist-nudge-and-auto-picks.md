---
id: TASK-24.3
title: >-
  P3-E04-T03 Scoring and buckets: facets, the weighted score, the playlist nudge
  and auto picks
status: To Do
assignee: []
created_date: '2026-10-07 07:16'
labels:
  - user-story
  - P3
milestone: m-3
dependencies:
  - TASK-24.2
references:
  - 'rekord-api/src/main/java/app/rekord/matcher/Score.java:24-157'
  - 'rekord-api/src/main/java/app/rekord/matcher/Matcher.java:44-214'
  - 'docs/rewrite/analysis/14-dj-matching-exports.md:121-137'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:174'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-24
priority: high
type: feature
ordinal: 30403
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want each candidate scored on title, artist, version and duration and the song decided only when one file clearly wins and carries the requested artist and title so that the matcher never swaps a requested song for another song, or a remix for an original, on its own.

The port of Score and Matcher.matchOne without remembered choices (P3-E05-T02 adds them): the facets and their weights (BR-MX-11), the version facet (BR-MX-12), the duration facet (BR-MX-13), the weighted mean rounded half-even to 4 decimals, the delta rounded half-even to 1 decimal and the 0.45 floor (BR-MX-14), the playlist nudge that orders but never decides (BR-MX-15, BR-LIB-42) and the three buckets (BR-MX-16, BR-MX-17). One change, decided by the user: auto also needs the best candidate's normalised artist and normalised core title, the signature of P3-E04-T01 without the version, to equal the query's, and a query without an artist is never auto; the margin, version and duration guards of rekord-api stay, every other result with a candidate at or above 0.60 is ambiguous, and a result with none is unmatched with the query's artist and title as entered (deviation UD-19.c; rekord-api answers auto on the three guards alone, Matcher.java:173-207). Every score below was computed with rekord-api's matcher classes. Unless a criterion says otherwise, the DJ's query is "Daft Punk" / "One More Time" at 320 seconds and the library files carry that artist and title.

- Covers: BR-MX-11, BR-MX-12, BR-MX-13, BR-MX-14, BR-MX-15, BR-MX-16, BR-MX-17, BR-LIB-42, PIN-14-0369, UD-19.c

Plan item `P3-E04-T03` (user-story,P3) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given the title pairs (Song, Song), (Song (A Remix), Song (A Remix)), (Song, Song (Extended Mix)), (Song, Song (A Remix)), (Song (A Remix), Song (B Remix)), (Song, Song (Remastered 2011)), (Song (Extended Mix), Song (A Remix)), (Song (Remix), Song (A Remix)), (Titel - Live - Radio Edit, Titel (Radio Edit)), (Song, Song (Live)) and (Song (Remastered), Song (Extended Mix)) When wedding-portal computes the version facet Then the values are 1.0, 1.0, 0.6, 0.25, 0.2, 0.9, 0.2, 0.85, 0.4, 0.25 and 0.54 (BR-MX-12; Score.java:90-126).
- [ ] #2 Given the duration pairs (200, 203), (200, 224), (200, 245) and (200, unknown) When wedding-portal computes the duration facet Then the values are 1.0, 0.5, 0.0 and null (BR-MX-13; Score.java:129-135).
- [ ] #3 Given one file at 322 seconds When the DJ's query is matched Then the bucket is auto with that file picked, its score is 1.0, its parts are title 1.0, artist 1.0, version 1.0 and duration 1.0, and duration_delta_sec is 2.0; with files at 320.25 and 320.75 seconds instead, duration_delta_sec is 0.2 and 0.8, rounded half-even (BR-MX-11, BR-MX-14, PIN-14-0369; Matcher.java:140-166, :209-214).
- [ ] #4 Given one file titled "One More Time (Kygo Remix)" at 320 seconds When the DJ's query is matched Then the file is listed with score 0.8875 and version facet 0.25 and the bucket is ambiguous; with a file titled "One More Time (Extended Mix)" at 400 seconds and a query without duration, the score is 0.9294, version 0.6, duration null and the bucket is ambiguous (BR-MX-11, BR-MX-14, BR-MX-16).
- [ ] #5 Given one file with no artist, title "Daft Punk One More Time" and no duration When the DJ's query without duration is matched, and a query of artist null against the tagged file "Daft Punk" / "One More Time" at 320 seconds is matched Then the first has parts combined 1.0, version 1.0 and duration null, score 1.0 and bucket ambiguous with auto_selected_id null, because the file's normalised artist is empty, and the second has artist part null, which drops out of the mean, and bucket ambiguous with auto_selected_id null, because a query without an artist is never auto (deviation UD-19.c; rekord-api answers auto to both; Matcher.java:140-154).
- [ ] #6 Given one file tagged "Daft Punk" / "Da Funk (Live)" at 500 seconds When the DJ's query is matched Then the file passes the token gate but scores below 0.45, so the candidates are empty and the bucket is unmatched; and given one file tagged "Other Band" / "One More Night" at 200 seconds the file is listed with score 0.4502 and the bucket is unmatched, because no candidate reaches 0.60 (BR-MX-14, BR-MX-17; Matcher.java:76-79, :201-206).
- [ ] #7 Given a file a at 320 seconds and a file b titled "One More Time (Kygo Remix)" at 320 seconds When the DJ's query is matched Then the list is a with 1.0, then b with 0.8875, and the bucket is auto with a, because a leads by at least 0.10; given a file at 380 seconds alone the score is 0.85 with duration 0.0 and the bucket is ambiguous, and given a file at 322 seconds and a query without duration the bucket is auto (BR-MX-16; Matcher.java:173-199).
- [ ] #8 Given two identical files x and y, in that library order, at 320 seconds When the DJ's query is matched Then the list is y then x, both 1.0, and the bucket is ambiguous with auto_selected_id null; when x is in the imported playlist "Peak hour" the list is x with playlists [Peak hour] then y, and the bucket is auto with x, because only the leader is in a playlist (BR-MX-15, BR-MX-16, BR-LIB-42).
- [ ] #9 Given a file a at 320 seconds in the playlist P1 and a file b at 326 seconds in the playlists P1, P2, P3 and P4 When the DJ's query is matched Then b is listed first with score 0.9893 and playlists [P1, P2, P3, P4], a second with 1.0, and the bucket is ambiguous, because the nudge orders and the bucket reads the raw scores of the first two (BR-MX-15, BR-LIB-42; Score.java:154-156).
- [ ] #10 Given a file a at 320 seconds in no playlist and a file b at 342.6 seconds with score 0.93 in the 4 playlists P1 to P4 When the DJ's query is matched Then a is listed first, because the nudge counts at most 3 playlists and b ranks 0.99, and the bucket is ambiguous, because a leads by only 0.07 (BR-MX-15, BR-LIB-42; Score.java:48-49).
- [ ] #11 Given 9 identical files at 320 seconds When the DJ's query is matched Then exactly 8 candidates are listed and the bucket is ambiguous (BR-MX-15; Score.java:34).
- [ ] #12 Given one file tagged "The Chainsmokers ft. Daya" / "Don't Let Me Down (Intro)" at 228 seconds When the query "The Chainsmokers ft. Daya" / "on't Let Me Down (Intro)" at 228 seconds is matched Then the file is listed with score 0.9163 and parts artist 1.0, version 1.0 and duration 1.0, and the bucket is ambiguous with auto_selected_id null, because the normalised core titles differ (deviation UD-19.c; rekord-api answers auto with that file; golden-set.json case 85).
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
