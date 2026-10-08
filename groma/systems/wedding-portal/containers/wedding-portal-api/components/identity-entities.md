---
type: C4 Component
title: Identity tables mapping
status: stable
groma:
  id: identity-entities
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/identity/UserEntity.java
      symbol: UserEntity
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/identity/OrganizationEntity.java
      symbol: OrganizationEntity
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/identity/MembershipEntity.java
      symbol: MembershipEntity
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/persistence/identity/SessionEntity.java
      symbol: SessionEntity
description: Hibernate entities of the organisation, account, membership and session tables.
---

Maps the four identity tables of the V2 migration (organizations, users, memberships, sessions) to plain entity classes, so Hibernate validates the schema at start-up and later repositories can persist through them. The entities hold fields, getters and setters and compare by id; they show no personal value or secret in toString. A session keeps only the SHA-256 hash of its token. Ports, repositories and mappers come with the sign-in tickets.
