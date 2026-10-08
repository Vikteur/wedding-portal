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
description: Removed by TASK-47; no code.
---

**Code removed by TASK-47 (2026-10-09).** The files listed under Code no longer exist: the product code was removed to start over. groma 0.6.6 cannot remove a component that holds scanned relationship rows, so this entry stays until a newer groma can remove it. Do not treat it as existing code.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [application/src/main/java/app/rekord/application/error/ErrorStatusTable.java](../../../../../../application/src/main/java/app/rekord/application/error/ErrorStatusTable.java) | [rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java](../../../../../../rekord-domain/src/main/java/app/rekord/domain/shared/error/RekordException.java) | Maps errors to HTTP statuses | Java call |
