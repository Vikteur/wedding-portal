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

Scores each candidate on title, artist, version and duration with fixed weights, rounded half-even to 4 decimals. A small playlist nudge (0.02 per playlist, at most 3) orders candidates but never changes a score, and a clear playlist leader passes the margin guard.
