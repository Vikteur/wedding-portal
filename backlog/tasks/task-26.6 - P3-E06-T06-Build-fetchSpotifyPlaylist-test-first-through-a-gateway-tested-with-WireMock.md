---
id: TASK-26.6
title: >-
  P3-E06-T06 Build fetchSpotifyPlaylist test-first through a gateway tested with
  WireMock
status: To Do
assignee: []
created_date: '2026-10-07 07:17'
labels:
  - user-story
  - P3
  - test-first
milestone: m-3
dependencies:
  - TASK-5
references:
  - 'rekord-api/src/main/java/app/rekord/spotify/SpotifyPlaylistFetch.java:15-76'
  - 'rekord-api/src/main/java/app/rekord/spotify/SpotifyEmbed.java:25-237'
  - 'rekord-api/src/main/java/app/rekord/matcher/MatchingResource.java:230-256'
  - 'rekord-api/src/test/java/app/rekord/spotify/SpotifyEmbedTest.java:20-170'
  - 'rekord-api/src/test/resources/spotify/embed-truncated.html:1-8'
  - 'rekord-contract/paths/dj.yaml:440-469'
  - 'rekord-contract/components/library.yaml:395-447'
  - 'docs/rewrite/architecture-conventions.md:642-655'
  - 'docs/rewrite/architecture-conventions.md:762'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:190-193'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:399'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:511'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-26
priority: high
type: feature
ordinal: 30606
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a DJ, I want to paste a public Spotify playlist link and get its songs back so that the couple's playlist becomes a list I can match against my files.

The operation reads the public embed page of a playlist anonymously: no account, no token, nothing downloaded, and only name, owner, total and each track's artist, title and duration (SpotifyPlaylistFetch.java:15-69, SpotifyEmbed.java:25-237). It accepts a bare 22-character id or an open.spotify.com playlist link in the shapes people paste (BR-LIB-49). The fetch is a GET with a browser User-Agent and Accept-Language en, a 10-second connect and a 15-second request timeout, following redirects (BR-LIB-50). Parsing reads the __NEXT_DATA__ JSON, finds the playlist entity even when it is re-nested, and reads title, artists and duration (BR-LIB-51); truncated reports a list cut short by the page (BR-LIB-52). Following the gateway rules (architecture-conventions.md:642-655), the wire client sits behind a gateway, the embed base address and the timeouts are configuration whose defaults are rekord-api's values (https://open.spotify.com/embed/playlist/, 10 s, 15 s), the dependency has its own circuit breaker, the retry count is configuration with a default of 0 (rekord-api makes one attempt), and the tests run against WireMock. An upstream 404 answers 404 SPOTIFY_FETCH_FAILED, as rekord-api does (PIN-13-0511, analysis 13 D9; the status table of architecture-conventions.md:762). rekord-api has no test for this operation and its upstream address is fixed (SpotifyPlaylistFetch.java:29), so the characterization test covers the refusals made before any network call (UD-15.d).

- Builds: `fetchSpotifyPlaylist`
- Test first: yes — pin the rekord-api behaviour with a characterization test before the build (UD-15.d)
- Covers: BR-LIB-49, BR-LIB-50, BR-LIB-51, BR-LIB-52, PIN-13-0511
- Error code `FORBIDDEN` without a criterion here: answered by the route guard of P0-E06 with 403 {"detail":{"code":"FORBIDDEN","message":"This is not yours to open."}} in the error envelope of P1-E09-T01 (deviation UD-19.i4, RISK-18 approved; rekord-api answers the role-denied 403 recorded in P0-E06-T02), to a session that holds none of the roles DJ, PLANNER and ADMIN (MatchingResource.java:52; ADMIN under UD-14.b3), such as a couple portal session; asserted for this operation by P3-E09-T01
- Error code `NOT_SIGNED_IN` without a criterion here: answered by the route guard of P0-E06 to a request without a session, before the operation runs; asserted for this operation by P3-E09-T01
- Error code `UNKNOWN` without a criterion here: the catch-all of P0-E05 for an unexpected failure only; the operation writes no rows
- Error code `VALIDATION_FAILED` without a criterion here: a request body or parameter that breaks the contract's constraints answers 422 VALIDATION_FAILED through P0-E05 (UD-12)

Plan item `P3-E06-T06` (user-story,P3,test-first) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given rekord-api as the behaviour oracle When a characterization test calls fetchSpotifyPlaylist with "https://open.spotify.com/album/37i9dQZF1DXcBWIGoYBM5M", with "https://example.com/nope" and with a body without url Then it records 400 BAD_URL with the message "That is a Spotify link, but not a playlist link. Use a URL like https://open.spotify.com/playlist/...", 400 BAD_URL with the message "That is not a Spotify playlist URL or playlist id." and 422 VALIDATION_FAILED, and the same test passes against wedding-portal before the operation counts as built (UD-15.d).
- [ ] #2 Given a WireMock stub for the embed path of the id 37i9dQZF1DXcBWIGoYBM5M When the DJ calls fetchSpotifyPlaylist with that id, with https://open.spotify.com/playlist/ plus the id, with that link plus "?si=abc123&pt=x", with the intl-nl link, with the link in double quotes and surrounding spaces, and with "spotify link: <link> enjoy" Then each call sends one GET to the embed path of that id with a browser User-Agent and Accept-Language en, verified by WireMock (BR-LIB-49; SpotifyEmbedTest.java:135-148).
- [ ] #3 Given a stub that answers a page built from rekord-api's fixture embed-playlist.html, with the owner name "Test Owner" When the DJ calls fetchSpotifyPlaylist Then the answer is 200 with name "Test Warmup", owner_name "Test Owner", total null, truncated false and 3 tracks with index 0, 1 and 2, the first by "Étienne de Crécy" titled "Am I Wrong" with duration_sec 213 from the bare duration 213000, the second with duration_sec 371 from durationMs 371000, the third with duration_sec null (BR-LIB-51; SpotifyEmbedTest.java:23-52).
- [ ] #4 Given stub pages whose entity holds a duration object with totalMilliseconds 180500, a bare duration 245, an entity with no subtitle and no name, and a track with no subtitle and the artists One and Two When the DJ fetches each Then duration_sec is 181 and 245 (a bare value under 10,000 is seconds), owner_name is null, name is "Spotify playlist", and the artist is "One, Two" (SpotifyEmbed.java:179-212; SpotifyEmbedTest.java:101-110).
- [ ] #5 Given a stub page with trackCount 342 and 5 tracks, one with no stated total and 100 tracks, one with no stated total and 1 track, and one with an empty track list When the DJ fetches each Then truncated is true with total 342, true, false, and false with tracks [] and answer 200 (BR-LIB-52; SpotifyEmbedTest.java:54-80, SpotifyEmbedTest.java:126-133).
- [ ] #6 Given a stub page whose entity sits under props.somethingNew.deeper.entity instead of props.pageProps.state.data.entity When the DJ calls fetchSpotifyPlaylist Then the answer is 200 with that entity's name and tracks (SpotifyEmbedTest.java:82-99).
- [ ] #7 Given a stub page with no __NEXT_DATA__ script, one whose script is not JSON and one whose data holds no track list When the DJ fetches each Then each answer is 502 SPOTIFY_PARSE_FAILED with the message text of SpotifyEmbed.java:112-118 word for word, which names the reason in parentheses and ends with the advice to paste the tracklist as text (BR-LIB-51; SpotifyEmbed.java:78-94).
- [ ] #8 Given a stub that answers 404 When the DJ calls fetchSpotifyPlaylist Then the answer is 404 SPOTIFY_FETCH_FAILED with the message "Spotify has no playlist at that link. If it is private, paste the tracklist as text instead." (PIN-13-0511; SpotifyPlaylistFetch.java:59-63).
- [ ] #9 Given a stub that answers 500, one that answers 403, one that resets the connection and one that answers after 16 seconds When the DJ fetches each Then each answer is 502 SPOTIFY_FETCH_FAILED with the message text of SpotifyPlaylistFetch.java:71-75 word for word (BR-LIB-50; SpotifyPlaylistFetch.java:49-75).
- [ ] #10 Given a stub that answers 302 to a second stub path serving a valid page When the DJ calls fetchSpotifyPlaylist Then the answer is 200 with that page's playlist (BR-LIB-50).
- [ ] #11 Given the gateway's circuit breaker opened by failing calls, with its thresholds set in the test's configuration When the DJ calls fetchSpotifyPlaylist Then the answer is 502 SPOTIFY_FETCH_FAILED and WireMock records no request for that call (architecture-conventions.md:646-647).
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
