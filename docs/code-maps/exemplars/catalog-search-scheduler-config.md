---
runtime: lazy
generated-by: pattern-scanner
source: storefront-adapter/src/main/java/com/acme/shop/storefront/adapter/config/CatalogSearchSchedulerConfig.java
serves: [scheduled-tasks, validation-notification-result]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `storefront-adapter/.../config/CatalogSearchSchedulerConfig.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A `@Configuration` class whose only job is to host `@Scheduled` batch jobs — kept separate from the
use-cases it schedules (`IndexCatalogUseCase`, `IndexPagesUseCase`), which stay callable and testable on
their own.

## `@Scheduled` cron job with a custom `@BatchJob` marker {#scheduled-tasks}
**Serves:** [`scheduled-tasks`](../scheduled-tasks.md)

`@Profile("!test")` on the class (jobs do not run during tests), `@Scheduled(cron = "...")`
per method — one method per job, explicit cron rather than `fixedDelay`, staggered start times
(`02:00` vs `02:10`) between related jobs to avoid overlap. A project-local `@BatchJob(name = "...")`
annotation (from the `logging` module) pairs with `@Scheduled` on every job for structured job-run
logging and auditing — local convention: any `@Scheduled` method is expected to also carry `@BatchJob`.
Job failures are signalled via a `Notification` result object (`hasErrors()` and `errorMessage()`) that
the method translates into a thrown `BatchJobFailedException`, not by letting the use-case throw
directly.

## Batch boundary for the validation notification result {#validation-notification-result}
**Serves:** `validation-notification-result`

The scheduled methods are a boundary in the collected-errors validation pattern: each use-case run
returns a `Notification` (`common-domain`), and the job method makes the one boundary decision —
`hasErrors()` means throw `BatchJobFailedException` with the joined `errorMessage()`. The use-case
itself never throws for expected validation failures; only the boundary converts the accumulated
result into control flow, so a partially failing indexing run reports every problem at once.

### Source (pseudonymized)
```java
package com.acme.shop.storefront.adapter.config;

import com.acme.shop.common.domain.validation.Notification;
import com.acme.shop.logging.BatchJob;
import com.acme.shop.logging.BatchJobFailedException;
import com.acme.shop.storefront.usecase.IndexCatalogUseCase;
import com.acme.shop.storefront.usecase.IndexPagesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Configuration
@Profile("!test")
public class CatalogSearchSchedulerConfig {

    private final IndexPagesUseCase indexPagesUseCase;
    private final IndexCatalogUseCase indexCatalogUseCase;

    @BatchJob(name = "Search index refresh - Catalog")
    @Scheduled(cron = "${typesense.scheduler.index-catalog-cron:0 0 2 * * *}")
    public void scheduleIndexCatalog() {
        Notification notification = indexCatalogUseCase.execute();
        if (notification.hasErrors()) {
            throw new BatchJobFailedException(notification.errorMessage());
        }
    }

    @BatchJob(name = "Search index refresh - Pages")
    @Scheduled(cron = "${typesense.scheduler.index-pages-cron:0 10 2 * * *}")
    public void scheduleIndexPages() {
        Notification notification = indexPagesUseCase.execute();
        if (notification.hasErrors()) {
            throw new BatchJobFailedException(notification.errorMessage());
        }
    }
}
```

### Edge cases
Catalog index run reports validation errors:
```java
when(indexCatalogUseCase.execute()).thenReturn(Notification.builder()
        .error("Catalog page lacks SKU 4380960")
        .build());
assertThatThrownBy(() -> config.scheduleIndexCatalog()).isInstanceOf(BatchJobFailedException.class);
```
Expected: the scheduler boundary throws immediately with the aggregated message.

Page indexing stays staggered from catalog indexing:
```properties
typesense.scheduler.index-catalog-cron=0 0 2 * * *
typesense.scheduler.index-pages-cron=0 10 2 * * *
```
Expected: the two jobs do not start at the same minute unless configuration explicitly changes them.

Tests activate the `test` profile:
```java
assertThat(CatalogSearchSchedulerConfig.class.getAnnotation(Profile.class).value())
        .containsExactly("!test");
```
Expected: scheduled jobs stay disabled in tests, so assertions can exercise the use-cases directly.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Scheduled' --glob '*.java'` (8 matches / 6 files / 4 modules)
