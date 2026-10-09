---
runtime: lazy
source: storefront-adapter/src/main/java/com/acme/shop/storefront/adapter/event/IndexPageEventListener.java
serves: [domain-events]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `storefront-adapter/.../event/IndexPageEventListener.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
Consumer side of the `storefront` domain-events pattern: reacts to `PageCreatedEvent` /
`PageUpdatedEvent` by re-indexing the page in Typesense. Its sibling `DeletePageEventListener`
removes deleted pages from the index.

## `@Observes(during = AFTER_SUCCESS)`, not plain `@Observes` {#domain-events}
**Serves:** [`domain-events`](../domain-events.md)

Every observer method takes its event as `@Observes(during = TransactionPhase.AFTER_SUCCESS)`.
The publisher fires the event synchronously inside the use case's `@Transactional` method, but
`AFTER_SUCCESS` defers the actual observer body until the surrounding transaction has committed — so a
Typesense indexing failure can never roll back the page save that already succeeded.

### Source (pseudonymized)
```java
package com.acme.shop.storefront.adapter.event;

import com.acme.shop.storefront.adapter.repository.exception.TypesenseException;
import com.acme.shop.storefront.domain.page.AbstractPage;
import com.acme.shop.storefront.events.PageCreatedEvent;
import com.acme.shop.storefront.events.PageUpdatedEvent;
import com.acme.shop.storefront.repository.SearchFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class IndexPageEventListener {

    private final SearchFacade searchFacade;

    public void indexPageOnUpdate(@Observes(during = TransactionPhase.AFTER_SUCCESS) final PageUpdatedEvent event) {
        indexPage(event.page(), "update");
    }

    public void indexPageOnCreation(@Observes(during = TransactionPhase.AFTER_SUCCESS) final PageCreatedEvent event) {
        indexPage(event.page(), "create");
    }

    private void indexPage(final AbstractPage page, final String operation) {
        try {
            searchFacade.indexPage(page);
        } catch (final TypesenseException e) {
            log.warn("Failed to {} Typesense index for page with id '{}'", operation, page.getId(), e);
        }
    }
}
```

### Edge cases
Update event arrives before commit completes:
```java
public void indexPageOnUpdate(@Observes(during = TransactionPhase.AFTER_SUCCESS) final PageUpdatedEvent event) {
```
Expected: observer execution waits until commit and never observes an uncommitted page.

Search backend throws during indexing:
```java
} catch (final TypesenseException e) {
    log.warn("Failed to {} Typesense index for page with id '{}'", operation, page.getId(), e);
}
```
Expected: the listener logs the failure and returns without rethrowing.

Shared helper handles both create and update:
```java
indexPage(event.page(), "create");
indexPage(event.page(), "update");
```
Expected: both entry points keep the same logging and error-handling behavior.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Observes\(during'` — see [create-or-update-static-page-usecase](create-or-update-static-page-usecase.md#domain-events) for the paired publisher.
