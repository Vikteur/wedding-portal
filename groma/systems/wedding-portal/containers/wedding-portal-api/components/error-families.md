---
type: C4 Component
title: Error families
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
  technology: Java 25
description: 'The domain''s typed failures: four exception families, each carrying a contract error code.'
---

A sealed `RekordException` has four families: not found, rejected (validation or conflict), not permitted, and upstream unavailable. Each carries an `ErrorCode` from the contract's fixed vocabulary (43 codes) and a message for the user, never an HTTP status, so the domain stays free of web concerns. A build test binds `ErrorCode` to the pinned contract enum. Nothing throws them yet.
