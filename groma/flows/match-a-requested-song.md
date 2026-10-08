---
type: Groma Flow
title: Match a requested song
groma:
  id: match-a-requested-song
---

How the matching domain resolves one song from a playlist to the DJ's own files, given a library index built from the library. The result is auto (one clear winner that is the requested song, auto-selected), ambiguous (the DJ picks from up to eight candidates) or unmatched. Implemented and tested in the domain, but no endpoint or use case starts it yet.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Track matcher](../systems/wedding-portal/containers/wedding-portal-api/components/track-matcher.md) | [Song identity](../systems/wedding-portal/containers/wedding-portal-api/components/song-identity.md) | Normalise the query's artist and title and split off its version |
| [Track matcher](../systems/wedding-portal/containers/wedding-portal-api/components/track-matcher.md) | [Library index](../systems/wedding-portal/containers/wedding-portal-api/components/library-index.md) | Retrieve candidates by shared tokens, or by the fuzzy fallback |
| [Track matcher](../systems/wedding-portal/containers/wedding-portal-api/components/track-matcher.md) | [Match scoring](../systems/wedding-portal/containers/wedding-portal-api/components/match-scoring.md) | Score each candidate on title, artist, version and duration, then bucket the best |
| [Track matcher](../systems/wedding-portal/containers/wedding-portal-api/components/track-matcher.md) | [Song identity](../systems/wedding-portal/containers/wedding-portal-api/components/song-identity.md) | Check the leader is the requested song before auto-picking it |
| [Track matcher](../systems/wedding-portal/containers/wedding-portal-api/components/track-matcher.md) | [Song identity](../systems/wedding-portal/containers/wedding-portal-api/components/song-identity.md) | Look up a remembered choice by the query's signature id and preselect its file |
