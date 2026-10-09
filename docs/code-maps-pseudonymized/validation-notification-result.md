---
runtime: lazy
generated-by: pattern-scanner
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map — `validation-notification-result` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`Notification.java`](exemplars/notification.md#validation-notification-result) | The collector itself: errors + warnings, `hasErrors()`, joined messages, `empty()` / `of()` / `merge()` / `addNotification()` composition | domain (`common-domain`) |
| [`CatalogSearchSchedulerConfig.java`](exemplars/catalog-search-scheduler-config.md#validation-notification-result) | Batch boundary: per-job `Notification` translated once into `BatchJobFailedException` | adapter (`storefront-adapter`) |

### Excerpts
Collector factories + composition helpers:
```java
public static Notification of(String error) {
    var notification = empty();
    notification.addError(error);
    return notification;
}

public Notification addNotification(Notification notification) {
    if (notification == null) {
        return this;
    }

    errors.addAll(notification.getErrors());
    warnings.addAll(notification.getWarnings());
    return this;
}
```

Batch boundary converts the collected result into control flow once:
```java
Notification notification = indexPagesUseCase.execute();
if (notification.hasErrors()) {
    throw new BatchJobFailedException(notification.errorMessage());
}
```

### Edge cases
Merging siblings keeps errors but not warnings:
```java
var left = Notification.of("Page.title is required");
var right = Notification.empty();
right.addWarning("Page.slug will be normalized");

var merged = Notification.merge(left, right);
```
Expected: `merged.getErrors()` contains both error lists; warning lists do not flow through `merge(...)`.

Adding a `null` child notification:
```java
var parent = Notification.empty();
parent.addNotification(null);
```
Expected: no exception and no state change.

Scheduled boundary receives multiple validation messages:
```java
var notification = Notification.empty();
notification.addError("Page.slug is duplicated");
notification.addError("Page.parentId creates a cycle");
throw new BatchJobFailedException(notification.errorMessage());
```
Expected: the batch job throws once with the comma-joined message instead of failing on the first error only.

## Local conventions (the project facts the skill omits)
- The result type is `com.acme.shop.common.domain.validation.Notification` (`common-domain`) — one shared
  class for the whole repo, never per-capability copies.
- Field rules go through validator helpers with the signature convention
  `StringValidator.notBlank("Type.field", value, notification)` — helpers append into the caller's
  accumulator rather than returning their own; one accumulator per validation pass.
- Entities expose `validate()` returning `Notification`; composite validations combine child
  results via `addNotification(...)` or `merge(...)`.
- Boundary translations: `@Scheduled` batch jobs check `hasErrors()` and throw
  `BatchJobFailedException`; creation flows validate before constructing.
- `merge(...)` propagates errors only; `addNotification(...)` propagates both errors and warnings.

## Frequency & coverage (why this earned a skill)
- Occurrences: 179 files across 32 modules import the type (as of `abc1234`) — one of the widest-spread
  shared patterns in the backend.

## Drift / exceptions
- Warning propagation differs by helper: `merge(...)` ignores warnings while `addNotification(...)`
  copies them. Callers pick the helper that matches the intent.

## Provenance
- Scanned at: `abc1234` · tool/query: `grep -rl 'common.domain.validation.Notification' --include='*.java'`
