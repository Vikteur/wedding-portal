---
type: C4 Actor
title: DJ
description: Plays the weddings they are assigned to and prepares the music for them.
status: stable
groma:
  id: dj
---

Uses the DJ app with a `DJ` account, which sees only the weddings the DJ is assigned to. Reads the couple's song lists and changes, keeps their own music library (sources, scans, imported playlists), matches requested songs to the files they own, and exports the finished playlist to rekordbox or a USB stick. The track-matching core exists in wedding-portal; the operations that reach it do not yet.

## Draft relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [dj](dj.md) | [wedding-portal](../systems/wedding-portal/system.md) | Matches and exports playlists | DJ app, HTTPS/JSON, session cookie |
