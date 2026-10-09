---
runtime: lazy
generated-by: pattern-scanner
source: account-adapter/src/main/java/com/acme/shop/account/adapter/controller/CustomerController.java
serves: [api-first-controller]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../controller/CustomerController.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A `@RestController` implementing a generated OpenAPI `*Api` interface (`AccountApi`) — the
contract-first shape used across every capability adapter.

## Contract-first controller {#api-first-controller}
**Serves:** [`api-first-controller`](../api-first-controller.md)

`public class CustomerController implements AccountApi` — endpoint methods are `@Override`s of the
generated interface (no `@GetMapping`/`@RequestBody` annotations re-declared on those methods; the
generated interface carries them). Constructor-injected use-cases, one per endpoint
(`GetDelegationsUseCase`, `GetCustomerInformationUseCase`, ...); the controller's own job is only to call
`.execute(...)` and map the domain result to the generated DTO via a small static `*ToDTOMapper`
(for example `DelegationToDTOMapper::map`), never building DTOs inline. One extra non-generated endpoint
(`verifyDelegationForTopic`, plain `@GetMapping`) shows the pattern also tolerates a controller-owned
route alongside the generated ones when the contract does not (yet) cover it.

### Source (pseudonymized)
```java
package com.acme.shop.account.adapter.controller;

import com.acme.shop.account.domain.Topic;
import com.acme.shop.account.usecase.GetCustomerInformationUseCase;
import com.acme.shop.account.usecase.GetDelegationsUseCase;
import com.acme.shop.account.usecase.RecordLastVisitCustomerUseCase;
import com.acme.shop.account.usecase.VerifyDelegationForTopicAccessUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class CustomerController implements AccountApi {
    private final Duration cacheTimeout;
    private final GetDelegationsUseCase getDelegationsUseCase;
    private final GetCustomerInformationUseCase getCustomerInformationUseCase;
    private final RecordLastVisitCustomerUseCase createOrUpdateCustomerLastVisitUseCase;
    private final VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase;

    public CustomerController(@Value("${cache.customer-profiles.expiration:1d}") Duration cacheTimeout,
                              GetDelegationsUseCase getDelegationsUseCase,
                              GetCustomerInformationUseCase getCustomerInformationUseCase,
                              RecordLastVisitCustomerUseCase createOrUpdateCustomerLastVisitUseCase,
                              VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase) {
        this.cacheTimeout = cacheTimeout;
        this.getDelegationsUseCase = getDelegationsUseCase;
        this.getCustomerInformationUseCase = getCustomerInformationUseCase;
        this.createOrUpdateCustomerLastVisitUseCase = createOrUpdateCustomerLastVisitUseCase;
        this.verifyDelegationForTopicAccessUseCase = verifyDelegationForTopicAccessUseCase;
    }

    @GetMapping("api/delegation/verify-topic-access/{topic}")
    public ResponseEntity<Boolean> verifyDelegationForTopic(@PathVariable Topic topic) {
        return ResponseEntity.ok()
                .body(verifyDelegationForTopicAccessUseCase.execute(topic));
    }

    @Override
    public ResponseEntity<List<DelegationDTO>> getAllDelegations() {
        var delegations = getDelegationsUseCase.execute().stream()
                .map(DelegationToDTOMapper::map).toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(cacheTimeout))
                .body(delegations);
    }

    @Override
    public ResponseEntity<CustomerInformationDTO> getGeneralInformation(String nid) {
        var customerInformation = getCustomerInformationUseCase.execute(nid);

        return ResponseEntity.ok().body(CustomerInformationToDTOMapper.map(customerInformation));
    }

    @Override
    public ResponseEntity<NotificationDTO> recordLastVisit() {
        var notification = createOrUpdateCustomerLastVisitUseCase.execute();

        if (notification.hasErrors()) {
            return ResponseEntity.badRequest().body(NotificationDTO.builder().errors(notification.getErrors()).build());
        }

        return ResponseEntity.ok().build();
    }
}
```

### Edge cases
Generated contract read with caching:
```java
var response = controller.getAllDelegations();
assertThat(response.getHeaders().getCacheControl()).contains("max-age");
```
Expected: generated-interface overrides can still add HTTP response metadata such as cache control.

Manual route beside generated methods:
```java
var response = controller.verifyDelegationForTopic(Topic.ORDERS);
assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
```
Expected: controller-owned routes can coexist with generated overrides without changing the contract-generated methods.

Write endpoint returning validation errors:
```java
when(recordLastCustomerVisitUseCase.execute()).thenReturn(Notification.builder()
        .error("Could not record Springfield storefront visit")
        .build());
var response = controller.recordLastVisit();
```
Expected: the controller turns notification errors into `400 Bad Request` with a structured `NotificationDTO`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@RestController' --glob '*.java'` (33 files / 14 modules)
