---
type: C4 Component
title: Error status table
status: stable
groma:
  id: error-status-table
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/error/ErrorStatusTable.java
  group: Errors
  technology: Java 25
description: Maps each error code and family to its HTTP status.
---

The only place where a domain error becomes an HTTP status, with the statuses `rekord-api` answers today (404, 400 to 429, 401/403, 502/503). A code can map to different statuses in different families. Building the table refuses a duplicate pair, and every new error code needs its row here. The error envelope mapper looks each family error up here to write its answer; a pair without a row answers 500.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [application/src/main/java/app/rekord/application/error/ErrorStatusTable.java](../../../../../../application/src/main/java/app/rekord/application/error/ErrorStatusTable.java) | [rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java) | Maps errors to HTTP statuses | Java call |
