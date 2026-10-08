---
type: C4 Component
title: First admin bootstrap
status: stable
groma:
  id: first-admin-bootstrap
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/security/FirstAdminBootstrap.java
      symbol: FirstAdminBootstrap
    - scanner: java
      file: application/src/main/java/app/rekord/application/security/BootstrapSettings.java
      symbol: BootstrapSettings
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/BootstrapFirstAdminUseCase.java
      symbol: BootstrapFirstAdminUseCase
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/BootstrapFirstAdminCommand.java
      symbol: BootstrapFirstAdminCommand
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/BootstrapOutcome.java
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/port/FirstAdminRepository.java
      symbol: FirstAdminRepository
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/port/NewFirstAdmin.java
      symbol: NewFirstAdmin
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/identity/DefaultFirstAdminRepository.java
      symbol: DefaultFirstAdminRepository
description: Creates the first business and its admin when the application starts
---

At start-up, reads the bootstrap settings and, when an address and a password are configured and no active admin exists yet, creates the first business, its admin account and its ADMIN membership. A password under 12 characters, a taken address or a business name whose slug is taken creates nothing and is logged without any personal data; the application still starts. Any other failure, such as the database being down, is not caught and stops the start. The use case holds the rules and the persistence adapter stores the three rows, refusing a taken address or business name by constraint name, and any other unique violation by its SQL state, so the refusal does not depend on the language of the database server. The address and the password come from the deployment secrets, never from git. The check for an admin and the creation are not one lock, so the bootstrap is meant for the first start of a single instance.
