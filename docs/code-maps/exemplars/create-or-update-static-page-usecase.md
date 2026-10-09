---
runtime: lazy
source: storefront-usecase/src/main/java/com/acme/shop/storefront/usecase/CreateOrUpdateStaticPageUseCase.java
serves: [domain-events]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `storefront-usecase/.../CreateOrUpdateStaticPageUseCase.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
One of the `CreateOrUpdate*UseCase` / `Delete*UseCase` classes in `storefront-usecase` that builds a
`PageCreatedEvent` / `PageUpdatedEvent` and publishes it only after the page is successfully saved.

## Publish-after-persist via `DomainEventPublisher` {#domain-events}
**Serves:** [`domain-events`](../domain-events.md)

The event (`DomainEvent createOrUpdateEvent`) is built during validation / creation but the
`domainEventPublisher.publish(...)` call happens strictly after `pageRepository.save(page)` and only
once `notification.hasErrors()` is false — never publish an event for a save that did not happen.
`DomainEvent` is an empty marker interface; `DomainEventPublisher` is the port with one method,
`publish(DomainEvent)`. The adapter-side implementation delegates to Spring's
`ApplicationEventPublisher`, so the use case stays framework-free.

### Source (pseudonymized)
```java
package com.acme.shop.storefront.usecase;

import com.acme.shop.common.domain.event.DomainEvent;
import com.acme.shop.common.domain.event.DomainEventPublisher;
import com.acme.shop.common.domain.validation.Creation;
import com.acme.shop.common.domain.validation.Notification;
import com.acme.shop.storefront.domain.page.CreateOrUpdateStaticPageRequest;
import com.acme.shop.storefront.domain.page.PageFactory;
import com.acme.shop.storefront.domain.page.PageGraph;
import com.acme.shop.storefront.domain.page.PageIdRepository;
import com.acme.shop.storefront.domain.page.ParentReference;
import com.acme.shop.storefront.domain.page.StaticPage;
import com.acme.shop.storefront.events.PageCreatedEvent;
import com.acme.shop.storefront.events.PageUpdatedEvent;
import com.acme.shop.storefront.repository.PageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.stream.Stream;

@Transactional
@RequiredArgsConstructor
public class CreateOrUpdateStaticPageUseCase {

    private final PageFactory pageFactory;
    private final PageRepository pageRepository;
    private final PageIdRepository pageIdRepository;
    private final DomainEventPublisher domainEventPublisher;

    public Notification execute(CreateOrUpdateStaticPageRequest request) {

        Notification notification;
        StaticPage page;
        DomainEvent createOrUpdateEvent;

        Optional<StaticPage> pageOptional = pageRepository.findStaticPage(request.getId());
        if (pageOptional.isPresent()) {
            page = pageOptional.get();
            notification = page.update(request, pageIdRepository.getAllPageIds());

            if (page.getParentId() != null) {
                var cyclicPagesNotification = new PageGraph(Stream.concat(
                                pageIdRepository.getAllParentReferences().stream(),
                                Stream.of(new ParentReference(page.getId(), page.getParentId())))
                        .toList()
                ).validateNoCyclicPages();
                notification.addNotification(cyclicPagesNotification);
            }

            createOrUpdateEvent = new PageUpdatedEvent(page);
        } else {
            Creation<StaticPage> creation = pageFactory.create(request);
            page = creation.getValue();
            notification = creation.getNotification();
            createOrUpdateEvent = new PageCreatedEvent(page);
        }

        if (notification.hasErrors()) {
            return notification;
        }

        pageRepository.save(page);

        domainEventPublisher.publish(createOrUpdateEvent);

        return Notification.empty();
    }

}
```

### Edge cases
Updating a page that introduces a parent cycle:
```java
if (page.getParentId() != null) {
    notification.addNotification(cyclicPagesNotification);
}
```
Expected: the cycle error joins the same notification and blocks both save and publish.

Creating a page with validation errors:
```java
Creation<StaticPage> creation = pageFactory.create(request);
notification = creation.getNotification();
```
Expected: the method returns the collected validation result without touching the repository.

Valid request on the update path:
```java
pageRepository.save(page);
domainEventPublisher.publish(createOrUpdateEvent);
return Notification.empty();
```
Expected: persistence happens before the event is published, and callers receive an empty success notification.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'DomainEvent'`, `rg '@TransactionalEventListener'`
