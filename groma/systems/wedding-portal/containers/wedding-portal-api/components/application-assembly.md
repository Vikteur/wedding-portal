---
type: C4 Component
title: ApiApplication
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
  group: REST API
---
