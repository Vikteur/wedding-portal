---
type: C4 System
title: Container runtime
description: Will run the backend image and check its liveness.
status: stable
groma:
  id: container-runtime
  technology: Docker
---

The Docker engine that will run the backend image. There is no image yet: TASK-47 (2026-10-09) removed the Dockerfile with the first implementation. The relationship below points at a file that no longer exists; groma 0.6.6 cannot remove a scanned relationship row, so it stays until a newer groma can remove it.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [container-runtime](container-runtime.md) | [rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java](../../rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java) | Probes liveness | HTTP GET /api/health |
