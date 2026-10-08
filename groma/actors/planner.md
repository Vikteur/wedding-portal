---
type: C4 Actor
title: Planner
description: Organises the weddings of an organisation and manages its team.
status: stable
groma:
  id: planner
---

Uses the planner web app. The `PLANNER` role owns the organisation: sees every wedding, plans its people, vendors, run sheet and tasks, invites and disables team members, and opens or revokes the couple portal. The planner may see how many songs the couple chose, but never which songs. None of these operations is implemented in wedding-portal yet; `rekord-api` still serves them.

## Draft relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [planner](planner.md) | [wedding-portal](../systems/wedding-portal/system.md) | Plans weddings and the team | planner web app, HTTPS/JSON, session cookie |
