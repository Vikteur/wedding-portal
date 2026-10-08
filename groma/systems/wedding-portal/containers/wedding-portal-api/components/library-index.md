---
type: C4 Component
title: Library index
status: stable
groma:
  id: library-index
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java
  group: Track matching
  technology: Java 25
description: Finds candidate files for a query in one DJ library through an inverted token index.
---

Built from a library's tracks in library order, it precomputes each track's normalised text and title parts and indexes their tokens. A candidate shares two query tokens or one token that is rare in the library; candidates come most hits first, capped at 300. When no file qualifies, a fuzzy fallback scans the whole library and keeps the closest few. Ties are broken by library position, so the order the tracks are loaded in matters.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java) | Normalises track text | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Fuzz.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Fuzz.java) | Ranks fallback candidates | Java call |
