---
runtime: lazy
source: application/src/main/java/com/acme/shop/exceptionhandling/RestExceptionHandler.java
serves: [exception-to-http]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `application/.../exceptionhandling/RestExceptionHandler.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The single central class of global `@ServerExceptionMapper` methods in the `application` module that all
capability adapters' controllers fall back to (there is a second, narrower one — `RestResponseEntityExceptionHandler` in
the same package — plus one capability-local mapper class in `vendor-relation-adapter`).

## Domain exception → HTTP status mapping {#exception-to-http}
**Serves:** [`exception-to-http`](../exception-to-http.md)

One `@ServerExceptionMapper` method per exception type, each returning
`RestResponse<Map<String, List<String>>>` with body shape `{"errors": [...]}` and an explicit
`Response.Status` — domain exceptions (`ConsentTransitionException`, `InvoiceCollectionAlreadyExistsException`) map to
`409`, authorization failures (`io.quarkus.security.ForbiddenException`) map to `403`, framework exceptions
(Jackson `JsonProcessingException`, `jakarta.ws.rs.BadRequestException`) map to `400`, `RateLimitException`
(SmallRye Fault Tolerance `@RateLimit`) maps to `429`. The catch-all `Exception` mapper returns a
`WebApplicationException`'s own response unchanged (status and entity) and
otherwise returns `500`. `jakarta.validation.ConstraintViolationException` deliberately has no mapper here —
Quarkus's built-in Hibernate Validator mapper is more specific than the catch-all, so its own
validation-error body format is kept as-is.

### Source (pseudonymized)
```java
package com.acme.shop.exceptionhandling;

import com.acme.shop.consent.domain.ConsentTransitionException;
import com.acme.shop.consent.domain.ConsentTypeUnavailableException;
import com.acme.shop.invoice.adapter.exception.InvoiceException;
import com.acme.shop.invoice.adapter.exception.InvoiceForbiddenException;
import com.acme.shop.invoice.domain.collection.InvoiceCollectionAlreadyExistsException;
import com.acme.shop.invoice.domain.collection.InvoiceCollectionNotFoundException;
import com.acme.shop.partner.exception.PartnerBadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.security.ForbiddenException;
import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleJsonProcessing(final JsonProcessingException ex) {
        LOGGER.warn("A message not readable exception occurred", ex);
        return RestResponse.status(Status.BAD_REQUEST, Map.of("errors", List.of("Request body could not be read")));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleBadRequest(final BadRequestException ex) {
        return RestResponse.status(Status.BAD_REQUEST, Map.of("errors", List.of("Request parameter does not match required type")));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleRateLimit(final RateLimitException ex) {
        LOGGER.warn("A rate limit exception occurred", ex);
        return RestResponse.status(Status.TOO_MANY_REQUESTS, Map.of("errors", List.of("Rate limit exceeded, please try again later")));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleForbiddenException(final ForbiddenException ex) {
        LOGGER.warn("An authorization exception occurred", ex);
        if (ex.getMessage() == null || ex.getMessage().contains("Access Denied")) {
            return RestResponse.status(Status.FORBIDDEN, Map.of("errors", List.of("Access Denied")));
        }
        return RestResponse.status(Status.FORBIDDEN, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handlePartnerBadRequest(final PartnerBadRequestException ex) {
        LOGGER.warn("Partner platform returned bad request", ex);
        return RestResponse.status(Status.BAD_REQUEST, Map.of("errors", List.of("Partner platform returned bad request")));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleConsentTransition(final ConsentTransitionException ex) {
        LOGGER.warn("A consent transition exception occurred", ex);
        return RestResponse.status(Status.CONFLICT, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleConsentTypeUnavailable(final ConsentTypeUnavailableException ex) {
        LOGGER.warn("A consent type unavailable exception occurred", ex);
        return RestResponse.status(Status.NOT_FOUND, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleInvoiceException(final InvoiceException ex) {
        LOGGER.warn("An invoice exception occurred", ex);
        return RestResponse.status(Status.BAD_REQUEST, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleInvoiceForbiddenException(final InvoiceForbiddenException ex) {
        LOGGER.warn("An invoice forbidden exception occurred", ex);
        return RestResponse.status(Status.FORBIDDEN, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleInvoiceCollectionNotFoundException(final InvoiceCollectionNotFoundException ex) {
        LOGGER.warn("An invoice collection not found exception occurred", ex);
        return RestResponse.status(Status.NOT_FOUND, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, List<String>>> handleInvoiceCollectionAlreadyExistsException(final InvoiceCollectionAlreadyExistsException ex) {
        LOGGER.warn("An invoice collection already exists exception occurred", ex);
        return RestResponse.status(Status.CONFLICT, Map.of("errors", List.of(ex.getMessage())));
    }

    @ServerExceptionMapper
    public Response handleDefault(final Exception ex) {
        if (ex instanceof WebApplicationException wae) {
            LOGGER.error("An exception occurred", ex);
            return wae.getResponse();
        }
        LOGGER.error("An unknown exception occurred", ex);
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("errors", List.of("An unknown exception occurred")))
                .build();
    }
}
```

### Edge cases
Framework validation exception:
```java
given().contentType(JSON).body("{\"customerName\": \"\"}")
        .when().post("api/delegation")
        .then().statusCode(400).body("violations", notNullValue());
```
Expected: `ConstraintViolationException` has no mapper in this class, so Quarkus keeps its built-in validation response shape.

Partner request rejected upstream:
```java
var response = handler.handlePartnerBadRequest(new PartnerBadRequestException("example.invalid rejected the payload"));
assertThat(response.getStatus()).isEqualTo(400);
```
Expected: callers receive a normalized partner-platform error body, not the raw upstream payload.

Exception that is a `WebApplicationException` carrying its own status:
```java
var response = handler.handleDefault(new NotFoundException());
assertThat(response.getStatus()).isEqualTo(404);
```
Expected: the catch-all keeps the exception's own status instead of turning it into a `500`.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg -l '@ServerExceptionMapper'` (3 files / 2 modules), `rg '@ServerExceptionMapper'` (11 matches / 3 files)
