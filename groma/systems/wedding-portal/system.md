---
type: C4 System
title: Wedding portal
status: stable
groma:
  id: wedding-portal
description: 'The Rekord backend: one REST API for the planner, the couple and their friends, and the DJ.'
---

The software system behind Rekord, a product for planning weddings and their music. A wedding is planned by a planner, filled in by the couple (and their friends, for one shared song list) and played by a DJ, who turns the requested songs into files from their own music library and exports the result. Its single REST API, defined by the rekord-contract OpenAPI spec, serves the planner web app, the couple portal and the DJ app; the frontends live in other repositories.

This repository is the Quarkus rewrite (wedding-portal) of the current backend rekord-api. It has no code yet: TASK-47 (2026-10-09) removed the first implementation to start over. The planned operations are recorded as draft relationships from the actors.
