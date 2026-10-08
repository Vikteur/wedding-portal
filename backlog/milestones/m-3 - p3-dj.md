---
id: m-3
title: "P3 DJ"
---

## Description

DJ backend: the DJ's weddings and change feed, libraries and sources, folder scan and rekordbox XML import, the track matcher with the golden-set gate, remembered choices, imported playlists, the Spotify playlist fetch, the playlist exports and the per-couple export. It builds the 26 DJ operations plus the three per-couple export operations of UD-16 UX-01, with the deviations of UD-19: auto matches only for an equal artist and core title (UD-19.c), getDjWedding without the portal token and the access code (UD-19.m1), synthetic /music/ paths in the golden fixtures (UD-19.m3), at most 1000 tracks per match request (UD-19.m5), normalisation of every script (UD-19.m6), export refusals of UD-19.m7, the role-denied 403 in the error envelope (UD-19.i4) and library-name races answered 409 DUPLICATE_NAME (UD-19.i5); the folder scan and track_count stay as in rekord-api (UD-19.m2, UD-19.m4).
