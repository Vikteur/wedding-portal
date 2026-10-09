---
runtime: lazy
generated-by: pattern-scanner
source: application/src/main/java/com/acme/shop/exceptionhandling/RestExceptionHandler.java
serves: [exception-to-http]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `application/.../exceptionhandling/RestExceptionHandler.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The single central `@ControllerAdvice` in the `application` module that all capability adapters'
controllers fall back to (there is a second, narrower one — `RestResponseEntityExceptionHandler` in
the same package — plus one capability-local advice in `vendor-relation-adapter`).

## Domain exception → HTTP status mapping {#exception-to-http}
**Serves:** [`exception-to-http`](../exception-to-http.md)

One `@ExceptionHandler` method per exception type, each returning
`ResponseEntity<Map<String, List<String>>>` with body shape `{"errors": [...]}` and an explicit
`HttpStatus` — domain exceptions (`ConsentTransitionException`, `InvoiceCollectionAlreadyExistsException`) map to
`409`, authorization failures map to `403`, framework exceptions (`HttpMessageNotReadableException`,
`MethodArgumentTypeMismatchException`) map to `400`, `RequestNotPermitted` (resilience4j rate
limiter) maps to `429`. The catch-all `@ExceptionHandler(Exception.class)` re-throws when the
exception already carries a `@ResponseStatus` (so Spring's default handling still applies) and
otherwise returns `500`. `BindException`/`MethodArgumentNotValidException` are deliberately
re-thrown, not translated — Spring's own validation-error body format is kept as-is.

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
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<String>>> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex) {
        LOGGER.warn("A message not readable exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of("Request body could not be read")), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getName() + " does not match required type")), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public void handleValidation(final BindException ex) throws BindException {
        LOGGER.warn("A validation exception occurred");
        throw ex;
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, List<String>>> handleRequestNotPermitted(final RequestNotPermitted ex) {
        LOGGER.warn("A rate limit exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of("Rate limit exceeded, please try again later")), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, List<String>>> handleAuthorizationDeniedException(final AuthorizationDeniedException ex) {
        LOGGER.warn("An authorization exception occurred", ex);
        if (ex.getMessage().contains("Access Denied")) {
            return new ResponseEntity<>(Map.of("errors", List.of("Access Denied")), HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PartnerBadRequestException.class)
    public ResponseEntity<Map<String, List<String>>> handlePartnerBadRequest(final PartnerBadRequestException ex) {
        LOGGER.warn("Partner platform returned bad request", ex);
        return new ResponseEntity<>(Map.of("errors", List.of("Partner platform returned bad request")), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConsentTransitionException.class)
    public ResponseEntity<Map<String, List<String>>> handleConsentTransition(final ConsentTransitionException ex) {
        LOGGER.warn("A consent transition exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConsentTypeUnavailableException.class)
    public ResponseEntity<Map<String, List<String>>> handleConsentTypeUnavailable(final ConsentTypeUnavailableException ex) {
        LOGGER.warn("A consent type unavailable exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvoiceException.class)
    public ResponseEntity<Map<String, List<String>>> handleInvoiceException(final InvoiceException ex) {
        LOGGER.warn("An invoice exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvoiceForbiddenException.class)
    public ResponseEntity<Map<String, List<String>>> handleInvoiceForbiddenException(final InvoiceForbiddenException ex) {
        LOGGER.warn("An invoice forbidden exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvoiceCollectionNotFoundException.class)
    public ResponseEntity<Map<String, List<String>>> handleInvoiceCollectionNotFoundException(final InvoiceCollectionNotFoundException ex) {
        LOGGER.warn("An invoice collection not found exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvoiceCollectionAlreadyExistsException.class)
    public ResponseEntity<Map<String, List<String>>> handleInvoiceCollectionAlreadyExistsException(final InvoiceCollectionAlreadyExistsException ex) {
        LOGGER.warn("An invoice collection already exists exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of(ex.getMessage())), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, List<String>>> handleDefault(final Exception ex) throws Exception {
        if (ex.getClass().getAnnotation(ResponseStatus.class) != null) {
            LOGGER.error("An exception occurred", ex);
            throw ex;
        }
        LOGGER.error("An unknown exception occurred", ex);
        return new ResponseEntity<>(Map.of("errors", List.of("An unknown exception occurred")), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### Edge cases
Framework validation exception:
```java
assertThatThrownBy(() -> handler.handleValidation(bindException))
        .isSameAs(bindException);
```
Expected: validation exceptions are re-thrown so Spring keeps its built-in validation response shape.

Partner request rejected upstream:
```java
var response = handler.handlePartnerBadRequest(new PartnerBadRequestException("example.invalid rejected the payload"));
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
```
Expected: callers receive a normalized partner-platform error body, not the raw upstream payload.

Exception already annotated with `@ResponseStatus`:
```java
assertThatThrownBy(() -> handler.handleDefault(annotatedException))
        .isSameAs(annotatedException);
```
Expected: the advice rethrows so Spring can honor the explicit annotation.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@ControllerAdvice'` (3 files / 2 modules), `rg '@ExceptionHandler'` (11 matches / 3 files)
