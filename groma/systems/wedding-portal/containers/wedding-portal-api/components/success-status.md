---
type: C4 Component
title: Success status
status: stable
groma:
  id: success-status
  parent: wedding-portal-api
  code:
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/web/shared/SuccessStatus.java
      symbol: SuccessStatus
    - scanner: java
      file: rekord-adapter/src/main/java/app/rekord/adapter/web/shared/SuccessStatusFilter.java
      symbol: SuccessStatusFilter
  group: REST API
  technology: Jakarta REST (Quarkus REST)
description: Applies each operation's contract success code (200, 201, 202 or 204) to the response.
---

Resources implement generated interfaces, so Quarkus ignores a status annotation on the implementing method. A resource instead records its code on the request-scoped `SuccessStatus`, and the `SuccessStatusFilter` response filter applies it, dropping the body on 204. It only changes a successful answer: an error status set after the code was recorded stands. It is the only place allowed to set a response status (ArchUnit rule A14). No resource uses it yet.
