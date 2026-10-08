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
description: Removed by TASK-47; no code.
---

**Code removed by TASK-47 (2026-10-09).** The files listed under Code no longer exist: the product code was removed to start over. groma 0.6.6 cannot remove a component that holds scanned relationship rows, so this entry stays until a newer groma can remove it. Do not treat it as existing code.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java) | Normalises track text | Java call |
| [rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/LibraryIndex.java) | [rekord-domain/src/main/java/app/rekord/domain/matching/Fuzz.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/matching/Fuzz.java) | Ranks fallback candidates | Java call |
