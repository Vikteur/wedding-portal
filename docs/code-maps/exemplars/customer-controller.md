---
runtime: lazy
source: account-adapter/src/main/java/com/acme/shop/account/adapter/controller/CustomerController.java
serves: [api-first-controller]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../controller/CustomerController.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A plain Quarkus REST resource class implementing a generated OpenAPI `*Api` interface (`AccountApi`,
JAX-RS, from openapi-generator `jaxrs-spec`) — the contract-first shape used across every capability adapter.

## Contract-first controller {#api-first-controller}
**Serves:** [`api-first-controller`](../api-first-controller.md)

`public class CustomerController implements AccountApi` — endpoint methods are `@Override`s of the
generated interface (no `@Path`/`@GET`/`@PathParam` annotations re-declared on those methods; the
generated interface carries them). Methods return what the generated interface declares (the DTO, or
`void` for an operation without a response body); a fixed `Cache-Control` comes from RESTEasy Reactive's
`@Cache`, and a non-200 status with a body is thrown as a `WebApplicationException` carrying its response,
which the central exception handler passes through unchanged.
Constructor-injected use-cases, one per endpoint
(`GetDelegationsUseCase`, `GetCustomerInformationUseCase`, ...); the controller's own job is only to call
`.execute(...)` and map the domain result to the generated DTO via a small static `*ToDTOMapper`
(for example `DelegationToDTOMapper::map`), never building DTOs inline. One extra non-generated endpoint
(`verifyDelegationForTopic`, hand-annotated `@GET @Path`) shows the pattern also tolerates a controller-owned
route alongside the generated ones when the contract does not (yet) cover it.

### Source (pseudonymized)
```java
package com.acme.shop.account.adapter.controller;

import com.acme.shop.account.domain.Topic;
import com.acme.shop.account.usecase.GetCustomerInformationUseCase;
import com.acme.shop.account.usecase.GetDelegationsUseCase;
import com.acme.shop.account.usecase.RecordLastVisitCustomerUseCase;
import com.acme.shop.account.usecase.VerifyDelegationForTopicAccessUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.Cache;

import java.util.List;

@RequiredArgsConstructor
public class CustomerController implements AccountApi {
    private final GetDelegationsUseCase getDelegationsUseCase;
    private final GetCustomerInformationUseCase getCustomerInformationUseCase;
    private final RecordLastVisitCustomerUseCase createOrUpdateCustomerLastVisitUseCase;
    private final VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase;

    @GET
    @Path("api/delegation/verify-topic-access/{topic}")
    public Boolean verifyDelegationForTopic(@PathParam("topic") Topic topic) {
        return verifyDelegationForTopicAccessUseCase.execute(topic);
    }

    @Override
    @Cache(maxAge = 86400)
    public List<DelegationDTO> getAllDelegations() {
        return getDelegationsUseCase.execute().stream()
                .map(DelegationToDTOMapper::map).toList();
    }

    @Override
    public CustomerInformationDTO getGeneralInformation(String nid) {
        var customerInformation = getCustomerInformationUseCase.execute(nid);

        return CustomerInformationToDTOMapper.map(customerInformation);
    }

    @Override
    public void recordLastVisit() {
        var notification = createOrUpdateCustomerLastVisitUseCase.execute();

        if (notification.hasErrors()) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity(NotificationDTO.builder().errors(notification.getErrors()).build())
                    .build());
        }
    }
}
```

### Edge cases
Generated contract read with caching:
```java
given().when().get("api/delegation")
        .then().statusCode(200).header("Cache-Control", containsString("max-age"));
```
Expected: generated-interface overrides can still add HTTP response metadata such as cache control.

Manual route beside generated methods:
```java
given().when().get("api/delegation/verify-topic-access/ORDERS")
        .then().statusCode(200);
```
Expected: controller-owned routes can coexist with generated overrides without changing the contract-generated methods.

Write endpoint returning validation errors:
```java
when(recordLastCustomerVisitUseCase.execute()).thenReturn(Notification.builder()
        .error("Could not record Springfield storefront visit")
        .build());
var thrown = catchThrowableOfType(controller::recordLastVisit, WebApplicationException.class);
assertThat(thrown.getResponse().getStatus()).isEqualTo(400);
```
Expected: the controller turns notification errors into `400 Bad Request` with a structured `NotificationDTO`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'Controller implements [A-Za-z]+Api' --glob '*.java'` (33 files / 14 modules)
