---
type: C4 Component
title: RekordException
status: stable
groma:
  id: error-families
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java
      symbol: RekordException
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/ErrorCode.java
      symbol: ErrorCode
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/NotFoundException.java
      symbol: NotFoundException
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/NotPermittedException.java
      symbol: NotPermittedException
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/RejectedException.java
    - scanner: java
      file: rekord-domain/src/main/java/app/rekord/domain/shared/error/UpstreamUnavailableException.java
      symbol: UpstreamUnavailableException
  group: Errors
---
