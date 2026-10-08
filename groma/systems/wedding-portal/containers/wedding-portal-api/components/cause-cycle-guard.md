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
description: Keeps an exception whose cause chain loops back from hanging Quarkus REST.
---

Quarkus REST 3.39.1 follows `getCause()` with no visited set after the error mapper has answered, so a request whose exception has a cause cycle keeps a worker thread busy and never gets a response. An interceptor on every resource class rethrows a redacted, acyclic copy of such an exception, so the error envelope mapper answers 500 `UNKNOWN` and logs its one line as for any unhandled failure. Exceptions without a cycle pass through untouched. A build-time extension binds the interceptor to every concrete class that has `@Path` on itself, a superclass or an interface, which is how the generated resource interfaces carry it. It covers only what a resource method throws, not filters, body readers or failures delivered later through a `Uni`; a cyclic `RekordException` or `WebApplicationException` answers 500 instead of its own status.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [application/src/main/java/app/rekord/application/error/AcyclicCauseInterceptor.java](../../../../../../application/src/main/java/app/rekord/application/error/AcyclicCauseInterceptor.java) | [application/src/main/java/app/rekord/application/error/RedactedCause.java](../../../../../../application/src/main/java/app/rekord/application/error/RedactedCause.java) | Throws a redacted copy | Java call |
