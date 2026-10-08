---
type: C4 Component
title: Health endpoint
status: stable
groma:
  id: health-endpoint
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java
      symbol: HealthResource
  group: REST API
  technology: Jakarta REST (Quarkus REST)
description: Answers GET /api/health with {"ok":true} as the liveness check.
---

Implements the generated `HealthApi` interface, so path, verb and media type come from the contract and a contract change to this operation stops the build. The operation is open (no session) and checks no dependency, so it reports liveness, not readiness. The image's Docker health check and the CI image check call it.
