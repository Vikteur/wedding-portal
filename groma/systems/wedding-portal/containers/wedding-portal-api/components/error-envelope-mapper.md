---
type: C4 Component
title: Error envelope mapper
status: stable
groma:
  id: error-envelope-mapper
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/error/ErrorEnvelopeMapper.java
      symbol: ErrorEnvelopeMapper
    - scanner: java
      file: application/src/main/java/app/rekord/application/error/RedactedCause.java
      symbol: RedactedCause
  group: Errors
  technology: Java 25
description: Writes every refusal as the rekord-api error envelope.
---

Turns each exception that leaves a resource into `{"detail":{"code","message"}}` with the status and `application/json` that `rekord-api` answers: the four error families (status from the error status table), sign-in and permission failures, validation failures, and any other throwable. Only a 500 is logged, once at ERROR, as a redacted copy of the cause that keeps class names and frames and drops every message, so personal data stays out of the log.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [application/src/main/java/app/rekord/application/error/ErrorEnvelopeMapper.java](../../../../../../application/src/main/java/app/rekord/application/error/ErrorEnvelopeMapper.java) | [application/src/main/java/app/rekord/application/error/ErrorStatusTable.java](../../../../../../application/src/main/java/app/rekord/application/error/ErrorStatusTable.java) | Looks up the status of a family error | Java call |
| [application/src/main/java/app/rekord/application/error/ErrorEnvelopeMapper.java](../../../../../../application/src/main/java/app/rekord/application/error/ErrorEnvelopeMapper.java) | [rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java) | Answers each family | Java call |
