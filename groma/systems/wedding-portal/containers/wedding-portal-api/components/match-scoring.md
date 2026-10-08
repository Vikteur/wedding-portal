---
type: C4 Component
title: Match scoring
status: stable
groma:
  id: match-scoring
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Score.java
      symbol: Score
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/matching/Fuzz.java
      symbol: Fuzz
  group: Track matching
  technology: Java 25
description: Scores a candidate file against a query and holds the matcher's tuned thresholds.
---

Scores four facets (title, artist, version, duration), combines them with fixed weights over the facets present, rounded half-even to 4 decimals, and defines the thresholds for showing, auto-picking and separating candidates. A small playlist nudge (0.02 per playlist, at most 3) orders candidates but never changes a score, and a clear playlist leader passes the margin guard. The fuzzy similarity measures are implemented here exactly as rapidfuzz defines them, because the thresholds were tuned against its output and a merely similar library would silently re-tune the matcher; a test checks them against 1,016 pairs recorded from the Python.
