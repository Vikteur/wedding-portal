---
type: C4 Container
title: Wedding portal API
description: The backend application that serves the Rekord REST API under /api.
status: draft
groma:
  id: wedding-portal-api
  parent: wedding-portal
  technology: Java 25, Quarkus 3.39 (REST, ArC, Hibernate ORM, Flyway)
---

The Quarkus application this repository builds (`:application:quarkusBuild`) and ships as a fast-jar Docker image listening on port 8080. The six Gradle modules (domain, use case, adapter, gateway, application, logging) are layers of this one application, not separate deployables; ArchUnit rules keep their dependencies pointing inward.

It implements the REST API of the pinned `rekord-contract` OpenAPI spec: the JAX-RS server interfaces are generated at build time and resources implement them, so contract drift breaks the compile. Today it serves the health operation under /api, and Quarkus' own /q/health* endpoints (the aggregate and /q/health/ready carry the database check) and the /q/metrics Prometheus endpoint sit outside /api, on the same port; the track-matching domain exists but no endpoint or use case reaches it yet, the use-case layer holds one use case, the first-admin bootstrap at start-up, whose repository adapter is the one place that raises the error model's refusals so far (a taken address or business name), and the gateway layer holds no business code. The error envelope mapper now answers every refusal raised inside JAX-RS in the `rekord-api` shape, except the 413 for an oversized body, the role-denied 403 and a `WebApplicationException` that carries its own entity. It is the from-scratch rewrite of `rekord-api`, which stays the running backend until each phase cuts over.

The box is a draft only because the Java scanner reports no execution entry for a Quarkus application (it has no `main` method), so a scan cannot confirm it.
