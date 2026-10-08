---
type: C4 Container
title: Wedding portal API
description: The backend application that will serve the Rekord REST API under /api.
status: draft
groma:
  id: wedding-portal-api
  parent: wedding-portal
  technology: Java 25, Quarkus (planned)
---

The Quarkus application this repository will build. There is no code yet: TASK-47 (2026-10-09) removed the first implementation so the tickets can be rebuilt with the agent framework. The planned layout (domain, use case, adapter, gateway, application and logging modules, with dependencies pointing inward) is in the umbrella's docs/rewrite/architecture-conventions.md. It will implement the REST API of the pinned rekord-contract OpenAPI spec and replace rekord-api phase by phase.
