---
type: Groma Flow
title: Liveness check
groma:
  id: liveness-check
---

The container runtime asks the running API whether it is alive; the health endpoint answers {"ok":true} without a session and without checking the database. Health is the only /api operation served today; readiness, which does check the database, is /q/health/ready, outside /api.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Container runtime](../externals/container-runtime.md) | [Health endpoint](../systems/wedding-portal/containers/wedding-portal-api/components/health-endpoint.md) | GET /api/health, answered 200 {"ok":true} |
