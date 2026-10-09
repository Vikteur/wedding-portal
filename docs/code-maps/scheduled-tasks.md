---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `scheduled-tasks` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`CatalogSearchSchedulerConfig.java`](exemplars/catalog-search-scheduler-config.md#scheduled-tasks) | `@Scheduled` cron job paired with a `@BatchJob` audit marker, `@Profile("!test")` | adapter (`storefront-adapter`) |

### Excerpts
One scheduled method per batch job:
```java
@BatchJob(name = "Search index refresh - Catalog")
@Scheduled(cron = "${typesense.scheduler.index-catalog-cron:0 0 2 * * *}")
public void scheduleIndexCatalog() {
    Notification notification = indexCatalogUseCase.execute();
    if (notification.hasErrors()) {
        throw new BatchJobFailedException(notification.errorMessage());
    }
}
```

Staggered companion job for page content:
```java
@BatchJob(name = "Search index refresh - Pages")
@Scheduled(cron = "${typesense.scheduler.index-pages-cron:0 10 2 * * *}")
public void scheduleIndexPages() {
    Notification notification = indexPagesUseCase.execute();
```

### Edge cases
Job disabled in tests:
```java
assertThat(CatalogSearchSchedulerConfig.class.isAnnotationPresent(Profile.class)).isTrue();
```
Expected: the configuration is guarded by `@Profile("!test")`, so scheduled jobs stay off in test runs.

Use-case reports multiple validation problems:
```java
when(indexCatalogUseCase.execute()).thenReturn(Notification.builder().error("Catalog page lacks SKU 4380960").build());
assertThatThrownBy(() -> config.scheduleIndexCatalog()).isInstanceOf(BatchJobFailedException.class);
```
Expected: the scheduler boundary throws once with the joined notification message instead of swallowing the errors.

Both cron properties left at defaults:
```properties
typesense.scheduler.index-catalog-cron=0 0 2 * * *
typesense.scheduler.index-pages-cron=0 10 2 * * *
```
Expected: the jobs run ten minutes apart to avoid overlap on the same search cluster.

## Local conventions (the project facts the skill omits)
- Package root: `<module>/.../config/` — jobs live in a dedicated `*SchedulerConfig`
  `@Configuration` class, separate from the `*UseCase` they invoke.
- Naming shape: `@Scheduled(cron = "...")` (explicit cron, not `fixedDelay`, in the sampled
  exemplar) + a project-local `@BatchJob(name = "...")` marker from the `logging` module on every
  scheduled method.
- Typesense index refresh cadence is externalized through
  `typesense.scheduler.index-catalog-cron` and `typesense.scheduler.index-pages-cron`. The default
  values remain 02:00 and 02:10; the DEV Helm deployment overrides them to 14:00 and 14:10, while
  local configuration follows DEV.
- Required collaborators / base types: the scheduled method delegates to a `*UseCase` and expects a
  `Notification` result (`hasErrors()`/`errorMessage()`), throwing `BatchJobFailedException` on
  failure rather than letting the use-case throw.
- Config / wiring: `@Profile("!test")` disables the job during tests (`application-test.yml`).

## Frequency & coverage (why this earned a skill)
- Occurrences: 8 `@Scheduled` matches across 6 files, 4 modules — `application`,
  `storefront-adapter`, `logging`, `account-adapter` (as of `abc1234`).

## Drift / exceptions
- None observed — every real `@Scheduled` site pairs with `@BatchJob`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Scheduled' --glob '*.java'` (excluding the annotation's own definition in `logging`)
