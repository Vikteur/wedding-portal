---
type: C4 Component
title: Track matcher
status: stable
groma:
  id: track-matcher
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/MatchQuery.java
      symbol: MatchQuery
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Bucket.java
      symbol: Bucket
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java
      symbol: QueryText
  group: Track matching
  technology: Java 25
description: Matches one requested playlist song to the DJ's library files and decides how sure the match is.
---

Entry point of the matching domain (`matchOne`): it normalises the query's artist and title, takes candidates from the library index, scores each one, keeps at most eight above the 0.45 floor, orders them (a small playlist nudge orders, scores stay raw) and buckets the result as auto, ambiguous (the DJ picks) or unmatched. The result is auto only when the leader clears the score, margin (or sole playlist member), version and duration guards and is the requested song: the same normalised artist and core title (UD-19.c). Playlist membership comes in as a track-id map. Remembered choices (P3-E05-T02) are the only part of `rekord-api`'s matcher not ported. No use case calls it yet.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | Retrieves candidate files | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Score.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Score.java) | Scores each candidate | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java) | Normalises the query | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Signature.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Signature.java) | Compares song identities | Java call |
