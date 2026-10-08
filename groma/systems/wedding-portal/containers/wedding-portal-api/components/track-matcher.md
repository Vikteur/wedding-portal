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

Entry point of the matching domain (`matchOne`): it normalises the query's artist and title, takes candidates from the library index, scores each one, keeps at most eight above the 0.45 floor, and buckets the result as auto (one clear, version-compatible winner, auto-selected), ambiguous (the DJ picks) or unmatched. Ported from `rekord-api`'s matcher; the playlist nudge, the duration delta, the later auto rule and remembered choices are not ported yet. No use case calls it yet.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | Retrieves candidate files | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Score.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Score.java) | Scores each candidate | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java) | Normalises the query | Java call |
