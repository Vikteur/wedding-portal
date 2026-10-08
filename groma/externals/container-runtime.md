---
type: C4 System
title: Container runtime
description: Runs the backend image and checks its liveness.
status: stable
groma:
  id: container-runtime
  technology: Docker
---

The Docker engine runs the fast-jar image as uid 10001 on port 8080 and, through the image's `HEALTHCHECK`, calls the health endpoint every 30 seconds. CI starts the image the same way on pushes to main to check it becomes healthy. The production host and orchestrator are not defined in this repository.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [container-runtime](container-runtime.md) | [rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java](../../rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java) | Probes liveness | HTTP GET /api/health |
