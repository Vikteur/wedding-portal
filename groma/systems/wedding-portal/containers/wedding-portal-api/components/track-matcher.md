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
      file: rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java
      symbol: QueryText
  group: Track matching
  technology: Java 25
description: Removed by TASK-47; no code.
---

**Code removed by TASK-47 (2026-10-09).** The files listed under Code no longer exist: the product code was removed to start over. groma 0.6.6 cannot remove a component that holds scanned relationship rows, so this entry stays until a newer groma can remove it. Do not treat it as existing code.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | Retrieves candidate files | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Score.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Score.java) | Scores each candidate | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/QueryText.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java) | Normalises the query | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/TrackMatcher.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Signature.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Signature.java) | Compares song identities | Java call |
