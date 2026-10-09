---
runtime: lazy
generated-by: pattern-scanner
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map — `domain-events` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`CreateOrUpdateStaticPageUseCase.java`](exemplars/create-or-update-static-page-usecase.md#domain-events) | Build the event, publish only after a successful save, via the `DomainEventPublisher` port | usecase (`storefront-usecase`) |
| [`IndexPageEventListener.java`](exemplars/index-page-event-listener.md#domain-events) | `@TransactionalEventListener(phase = AFTER_COMMIT)` consumer, failure caught and not rethrown | adapter (`storefront-adapter`) |

### Excerpts
Publish only after persistence succeeds:
```java
if (notification.hasErrors()) {
    return notification;
}

pageRepository.save(page);
domainEventPublisher.publish(createOrUpdateEvent);
```

Consume after commit so indexing cannot roll back the write:
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void indexPageOnCreation(final PageCreatedEvent event) {
    indexPage(event.page(), "create");
}
```

### Edge cases
Existing page creates a cycle through its new parent:
```java
var cyclicPagesNotification = new PageGraph(Stream.concat(
                pageIdRepository.getAllParentReferences().stream(),
                Stream.of(new ParentReference(page.getId(), page.getParentId())))
        .toList()
).validateNoCyclicPages();
notification.addNotification(cyclicPagesNotification);
```
Expected: the cycle is added to the same `Notification`; `save(...)` and `publish(...)` are skipped.

Creation validation fails before persistence:
```java
Creation<StaticPage> creation = pageFactory.create(request);
notification = creation.getNotification();

if (notification.hasErrors()) {
    return notification;
}
```
Expected: no event is published for a page that never reaches the repository.

Search index update fails after the transaction commits:
```java
try {
    searchFacade.indexPage(page);
} catch (final TypesenseException e) {
    log.warn("Failed to {} Typesense index for page with id '{}'", operation, page.getId(), e);
}
```
Expected: the failure is logged, the committed page stays stored, and the listener does not rethrow.

## Local conventions (the project facts the skill omits)
- Package root: marker interface + port in `common-domain/src/main/java/com/acme/shop/common/domain/event/`
  (`DomainEvent`, `DomainEventPublisher`); the Spring adapter in
  `common-adapter/src/main/java/com/acme/shop/common/adapter/event/SpringEventPublisher.java`;
  concrete events and publishing use cases live per capability, for example
  `storefront-usecase/.../events/Page{Created,Updated,Deleted}Event.java`.
- Naming shape: `Page{Created,Updated,Deleted}Event` (a `record` implementing `DomainEvent`, carrying the
  domain object); publishing use cases named `CreateOrUpdate*UseCase` / `Delete*UseCase`; listeners named
  `*EventListener` in `storefront-adapter/.../event/`.
- Required collaborators / base types: publish only after the write succeeds and only when
  `notification.hasErrors()` is false; consume via `@TransactionalEventListener(phase =
  TransactionPhase.AFTER_COMMIT)`, not `@EventListener`.
- Config / wiring: `SpringEventPublisher` is the only `DomainEventPublisher` implementation,
  delegating to Spring's `ApplicationEventPublisher` so use-case code stays framework-free.

## Frequency & coverage (why this earned a skill)
- Occurrences (as of `abc1234`): `rg 'DomainEvent'` — 51 matches / 18 files (including tests); `rg
  '@TransactionalEventListener'` — 4 matches / 3 files. Main-only: 13 files / 4 modules
  (`common-domain`, `common-adapter`, `storefront-usecase`, `storefront-adapter`).
- Hotspot: `storefront-usecase/.../events/` and `.../usecase/`.

## Drift / exceptions
- Listeners intentionally swallow failures (log + continue) rather than fail the transaction — the
  search index update is best-effort, not a source of truth.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'DomainEvent'` (51/18), `rg 'DomainEventPublisher'` (25/13), `rg '@TransactionalEventListener'` (4/3)
