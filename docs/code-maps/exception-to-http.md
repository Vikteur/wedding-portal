---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `exception-to-http` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`RestExceptionHandler.java`](exemplars/rest-exception-handler.md#exception-to-http) | One central class, one `@ServerExceptionMapper` method per exception type, `{"errors": [...]}` body shape | application |

### Excerpts
Central translation to the repo-wide error body (full source in the [exemplar leaf](exemplars/rest-exception-handler.md#exception-to-http)):
```java
@ServerExceptionMapper
public RestResponse<Map<String, List<String>>> handleJsonProcessing(final JsonProcessingException ex) {
    LOGGER.warn("A message not readable exception occurred", ex);
    return RestResponse.status(Status.BAD_REQUEST, Map.of("errors", List.of("Request body could not be read")));
}
```

Catch-all that preserves explicit Quarkus semantics:
```java
@ServerExceptionMapper
public Response handleDefault(final Exception ex) {
    if (ex instanceof WebApplicationException wae) {
        LOGGER.error("An exception occurred", ex);
        return wae.getResponse();
    }
```

### Edge cases
Framework validation exception:
```java
given().contentType(JSON).body("{\"customerName\": \"\"}")
        .when().post("api/delegation")
        .then().statusCode(400).body("violations", notNullValue());
```
Expected: `ConstraintViolationException` is left to Quarkus's own mapper so its validation-error response format is kept.

Authorization failure with the stock access-denied message:
```java
var response = handler.handleForbiddenException(new ForbiddenException("Access Denied"));
assertThat(response.getStatus()).isEqualTo(403);
assertThat(response.getEntity().get("errors")).containsExactly("Access Denied");
```
Expected: the handler returns the normalized stock message instead of echoing extra framework detail.

Exception that is a `WebApplicationException` carrying its own status:
```java
var response = handler.handleDefault(new NotFoundException());
assertThat(response.getStatus()).isEqualTo(404);
```
Expected: the mapper gets out of the way and keeps the exception's declared HTTP status.

## Local conventions (the project facts the skill omits)
- Package root: `application/src/main/java/com/acme/shop/exceptionhandling/`.
- Naming shape: `RestExceptionHandler` (primary), a narrower
  `RestResponseEntityExceptionHandler` sibling in the same package, plus one capability-local
  `@ServerExceptionMapper` class in `vendor-relation-adapter` for relation-specific exceptions.
- Required collaborators / base types: response body is always
  `RestResponse<Map<String, List<String>>>` with key `"errors"` — no problem-details/RFC-7807
  shape in this codebase.
- Config / wiring: `@ServerExceptionMapper` methods on a plain class outside any resource class —
  global, applies repo-wide.

## Frequency & coverage (why this earned a skill)
- Occurrences: 3 `@ServerExceptionMapper` classes across 2 modules (`application`,
  `vendor-relation-adapter`); 11 `@ServerExceptionMapper` matches across those 3 files (as of
  `abc1234`).

## Drift / exceptions
- `jakarta.validation.ConstraintViolationException` is deliberately not mapped to the
  `{"errors": [...]}` shape — Quarkus's own validation-error response format is kept. Not a gap;
  intentional.
- Quarkus ships a built-in mapper for Jackson's `MismatchedInputException`, which beats the
  `JsonProcessingException` mapper for that subtype; disable it with
  `quarkus.rest.exception-mapping.disable-mapper-for` if the `{"errors": [...]}` shape is wanted there too.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg -l '@ServerExceptionMapper'`, `rg '@ServerExceptionMapper'` (both `--glob '*.java'`)
