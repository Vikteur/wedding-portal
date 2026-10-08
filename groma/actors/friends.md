---
type: C4 Actor
title: Couple's friends
description: Add songs to the couple's shared friends list.
status: stable
groma:
  id: friends
---

The couple's friends open the shared friends link of the couple portal. A friends session sees only the couple's names, the date and the shared list, and may add songs to it; the couple keeps the final word on order. Not implemented in wedding-portal yet.

## Draft relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [friends](friends.md) | [wedding-portal](../systems/wedding-portal/system.md) | Adds songs to the shared list | couple portal friends link, HTTPS/JSON, portal cookie |
