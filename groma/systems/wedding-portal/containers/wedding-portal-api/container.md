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

It implements the REST API of the pinned `rekord-contract` OpenAPI spec: the JAX-RS server interfaces are generated at build time and resources implement them, so contract drift breaks the compile. Today it serves only the health operation; the track-matching domain and the error model exist but no endpoint or use case reaches them yet, and the use-case and gateway layers hold no business code. It is the from-scratch rewrite of `rekord-api`, which stays the running backend until each phase cuts over.

The box is a draft only because the Java scanner reports no execution entry for a Quarkus application (it has no `main` method), so a scan cannot confirm it.
