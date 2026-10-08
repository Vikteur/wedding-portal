---
id: TASK-19.3
title: P2-E04-T03 Search songs from the portal (searchSongs)
status: To Do
assignee: []
created_date: '2026-10-07 07:14'
labels:
  - user-story
  - P2
  - stop-auth-access
  - stop-crypto-logging
milestone: m-2
dependencies:
  - TASK-17.1
references:
  - 'rekord-api/src/main/java/app/rekord/spotify/SpotifySearch.java:53-175'
  - 'rekord-api/src/main/java/app/rekord/spotify/SpotifySearch.java:176-235'
  - 'rekord-api/src/main/java/app/rekord/portal/PortalResource.java:190-194'
  - 'rekord-contract/paths/portal.yaml:205-240'
  - 'docs/rewrite/analysis/12-couple-portal.md:102'
  - 'docs/rewrite/analysis/12-couple-portal.md:204'
  - 'docs/rewrite/analysis/12-couple-portal.md:262'
  - 'docs/rewrite/analysis/13-dj-library-ingestion.md:486-512'
documentation:
  - docs/rewrite/architecture-conventions.md
  - docs/rewrite/backlog-plan/ticket-rules.md
  - docs/rewrite/analysis/41-domain-model-and-glossary.md
parent_task_id: TASK-19
priority: high
type: feature
ordinal: 20403
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**User Story:** As a partner of the couple, I want to search for songs while I fill in our lists so that each wish names the exact track.

Song search through the Spotify Web API, tested against WireMock and never against Spotify itself. Oracle: SpotifySearch.java:85-175 with PortalResource.java:190-194. The contract requires q with minLength 2 (rekord-contract/paths/portal.yaml:223-228), enforced by request validation before any upstream call (P4, PIN-13-0492). The cache and the miss limiter live in the memory of one instance. rekord-api limits misses per portal although its comment says per session; the per-portal rule is kept. rekord-api writes the query text to its logs when the upstream call fails (SpotifySearch.java:161); that line is not reproduced (slice 12 R16, a STOP item on what gets logged). BR-OPS-31 is kept: throttling is keyed per portal only, never per client address, and UD-18.f adds no per-client or global throttle. BR-LIB-53 is kept: the contract's minLength 2 is checked before anything else, and the search configuration is checked before the length of the normalised query (SpotifySearch.java:91-99).

- Builds: `searchSongs`
- STOP (human approval in the pull request): auth-access, crypto-logging
- Covers: BR-CP-29, PIN-12-0204, PIN-12-0262, PIN-13-0492, BR-LIB-53, BR-OPS-31
- Error code `UNKNOWN` without a criterion here: 500 UNKNOWN is the catch-all answer for an unexpected failure (P0-E05); no business rule of this operation raises it, and no criterion provokes it.

Plan item `P2-E04-T03` (user-story,P2,stop-auth-access,stop-crypto-logging) in docs/rewrite/backlog-plan.json.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Given a COUPLE session and WireMock answering the Spotify search with 8 tracks When the partner calls searchSongs with q "dancing queen" Then WireMock receives one search request with q "dancing queen", type track and limit 8, and the answer is 200 with the 8 results in WireMock's order, each with spotify_id, uri, isrc, title, artist (the artists joined with ", "), duration_ms, the smallest image as art_url, and album.
- [ ] #2 Given a COUPLE session with search configured and one with search unconfigured When searchSongs is called from each without q and with q "a", and from the configured one with q of two spaces and with q "a " Then the first 4 answer 422 VALIDATION_FAILED, the last 2 answer 200 with an empty results list, and WireMock receives no request (slice 12 P4, PIN-13-0492).
- [ ] #3 Given a COUPLE session with the client secret blank When searchSongs is called with q "abba" and with q of two spaces Then both answer 503 SEARCH_UNAVAILABLE "Song search is offline — your text is saved exactly as typed.".
- [ ] #4 Given a COUPLE session When searchSongs is called with q "Dancing  Queen" and 9 minutes later from a second wedding's session with q " dancing queen" Then WireMock receives one request, because the cache key is the trimmed, single-spaced, lower-case query and the cache is shared by every wedding.
- [ ] #5 Given a COUPLE session and q "abba" cached at 12:00 When searchSongs is called with q "abba" again at 12:10:01 Then WireMock receives a second request, because a cache entry lives 10 minutes.
- [ ] #6 Given q "abba" cached first and then 512 other distinct queries cached, at most 20 per portal within any 10 seconds, without "abba" being read again When searchSongs is called with q "abba" Then WireMock receives a new request for it, because the cache keeps the 512 most recently used queries.
- [ ] #7 Given two COUPLE sessions of the first wedding on two devices, a FRIENDS session of the same wedding and a session of another wedding When the two COUPLE sessions make 21 uncached searches within 10 seconds, and then the FRIENDS session and the other wedding's session make one uncached search each Then the 21st answers 429 RATE_LIMITED "Slow down a moment, then try again.", a cached query from a COUPLE session still answers 200, and both later searches answer 200, because the limit of 20 misses per 10 seconds counts per portal.
- [ ] #8 Given WireMock answering the search with 429 When searchSongs is called Then the answer is 429 RATE_LIMITED "Spotify asked us to slow down — try again in a moment.".
- [ ] #9 Given WireMock answering the search with 500, and once delaying its answer past 10 seconds When searchSongs is called Then both answer 503 SEARCH_UNAVAILABLE "Song search is offline — your text is saved exactly as typed.".
- [ ] #10 Given no client token fetched yet and WireMock answering the first search with 401 and the retry with 200, and in a second test from the same start WireMock answering both searches with 401 When searchSongs is called with q "abba" in each test Then in the first WireMock's token endpoint receives 2 requests (the first token and the refresh), its search endpoint receives 2, and the answer is 200, and in the second the token endpoint receives 2 requests, the search endpoint receives 2, and the answer is 503 SEARCH_UNAVAILABLE.
- [ ] #11 Given WireMock failing the search with a connection reset When searchSongs is called with q "our secret song" Then no log line of wedding-portal holds "our secret song", the portal token or the client token (deviation slice 12 R16; rekord-api logs the query text at SpotifySearch.java:161).
- [ ] #12 Given the same wedding When searchSongs is called with q "abba" without a cookie, with another wedding's token and this wedding's cookie, and with a 10-character token Then the answers are 401 NOT_SIGNED_IN, 401 BAD_LINK and 422 VALIDATION_FAILED.
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
