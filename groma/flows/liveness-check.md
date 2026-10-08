---
type: Groma Flow
title: Liveness check
groma:
  id: liveness-check
---

The container runtime asks the running API whether it is alive; the health endpoint answers {"ok":true} without a session and without checking the database. This is the only request path the API serves today.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Container runtime](../externals/container-runtime.md) | [Health endpoint](../systems/wedding-portal/containers/wedding-portal-api/components/health-endpoint.md) | GET /api/health, answered 200 {"ok":true} |
