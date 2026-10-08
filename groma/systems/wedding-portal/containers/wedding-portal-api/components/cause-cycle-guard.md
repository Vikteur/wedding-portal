---
type: C4 Component
title: Cause-cycle guard
status: stable
groma:
  id: cause-cycle-guard
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/error/AcyclicCauseInterceptor.java
      symbol: AcyclicCauseInterceptor
  group: Errors
  technology: CDI interceptor, CDI build compatible extension
description: Removed by TASK-47; no code.
---

**Code removed by TASK-47 (2026-10-09).** The files listed under Code no longer exist: the product code was removed to start over. groma 0.6.6 cannot remove a component that holds scanned relationship rows, so this entry stays until a newer groma can remove it. Do not treat it as existing code.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [application/src/main/java/app/rekord/application/error/AcyclicCauseInterceptor.java](../../../../../../application/src/main/java/app/rekord/application/error/AcyclicCauseInterceptor.java) | [application/src/main/java/app/rekord/application/error/RedactedCause.java](../../../../../../application/src/main/java/app/rekord/application/error/RedactedCause.java) | Throws a redacted copy | Java call |
