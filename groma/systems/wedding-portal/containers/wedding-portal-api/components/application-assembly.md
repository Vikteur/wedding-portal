---
type: C4 Component
title: Application assembly
status: stable
groma:
  id: application-assembly
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/config/ApiApplication.java
      symbol: ApiApplication
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/shared/AdapterModuleProbe.java
      symbol: AdapterModuleProbe
    - scanner: java
      file: rekord-gateway/src/main/java/app/rekord/gateway/shared/GatewayModuleProbe.java
      symbol: GatewayModuleProbe
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/shared/UseCaseModuleProbe.java
      symbol: UseCaseModuleProbe
  technology: Quarkus, Jakarta REST
description: Roots the REST API under /api and proves the library modules' beans are discovered.
---

The JAX-RS `Application` puts every resource under `/api`, as the contract's `servers: /api` requires, while Quarkus' own `/q/*` endpoints stay at the root. The adapter, use-case and gateway modules each hold a placeholder bean that a test injects to prove their Jandex index makes their beans visible to the application; the placeholders go once those modules have real beans.
