---
type: C4 Component
title: Song identity
status: stable
groma:
  id: song-identity
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Normalize.java
      symbol: Normalize
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Versions.java
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Signature.java
      symbol: Signature
  group: Track matching
  technology: Java 25
description: Normalises song text, splits titles into core and version, and derives a song's stable signature id.
---

Folds artist and title text to lower-case words (keeping non-Latin letters), splits a title such as `Song (X Remix)` into its core name, version descriptors and remixer, and derives the signature that identifies "the same song in any playlist" (featured artists left out). Remembered version choices will be keyed by that signature id, so the output must stay byte-exact: any drift orphans them silently. The matcher and the library index rely on the normalisation and title split. It also gives the song without its version (normalised artist and core title), which the matcher compares before an auto pick (UD-19.c); that key is compared, never stored.
