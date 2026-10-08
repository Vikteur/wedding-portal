---
type: C4 Component
title: Clock and generators
status: stable
groma:
  id: clock-and-generators
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/config/ClockProducer.java
      symbol: ClockProducer
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/shared/port/IdGenerator.java
      symbol: IdGenerator
    - scanner: java
      file: application/src/main/java/app/rekord/application/config/RandomIdGenerator.java
      symbol: RandomIdGenerator
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/shared/port/TokenGenerator.java
      symbol: TokenGenerator
    - scanner: java
      file: application/src/main/java/app/rekord/application/config/SecureRandomTokenGenerator.java
      symbol: SecureRandomTokenGenerator
description: The application clock and the id and token generators
---

Provides the one system clock in UTC, a random id generator and a secure random token generator behind the use-case ports, so use cases never create time, ids or random bytes themselves.
