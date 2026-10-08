---
type: C4 Actor
title: Couple
description: Fills in their wedding intake and the songs they want.
status: stable
groma:
  id: couple
---

Uses the couple portal, signed in through a magic link plus a portal session rather than an account. Fills in the intake, orders and blocks songs on their lists, and sees their timeline and tasks. Not implemented in wedding-portal yet; `rekord-api` still serves it.

## Draft relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [couple](couple.md) | [wedding-portal](../systems/wedding-portal/system.md) | Fills in the intake | couple portal, HTTPS/JSON, magic link and portal cookie |
