---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `exception-to-http` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`RestExceptionHandler.java`](exemplars/rest-exception-handler.md#exception-to-http) | Central `@ControllerAdvice`, one `@ExceptionHandler` per exception type, `{"errors": [...]}` body shape | application |

### Excerpts
Central translation to the repo-wide error body (full source in the [exemplar leaf](exemplars/rest-exception-handler.md#exception-to-http)):
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, List<String>>> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex) {
    LOGGER.warn("A message not readable exception occurred", ex);
    return new ResponseEntity<>(Map.of("errors", List.of("Request body could not be read")), HttpStatus.BAD_REQUEST);
}
```

Catch-all that preserves explicit Spring semantics:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, List<String>>> handleDefault(final Exception ex) throws Exception {
    if (ex.getClass().getAnnotation(ResponseStatus.class) != null) {
        LOGGER.error("An exception occurred", ex);
        throw ex;
    }
```

### Edge cases
Framework validation exception:
```java
assertThatThrownBy(() -> handler.handleValidation(bindException))
        .isSameAs(bindException);
```
Expected: validation exceptions are re-thrown so Spring keeps its own validation-error response format.

Authorization failure with the stock access-denied message:
```java
var response = handler.handleAuthorizationDeniedException(new AuthorizationDeniedException("Access Denied"));
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
assertThat(response.getBody().get("errors")).containsExactly("Access Denied");
```
Expected: the handler returns the normalized stock message instead of echoing extra framework detail.

Exception already annotated with `@ResponseStatus`:
```java
assertThatThrownBy(() -> handler.handleDefault(annotatedException))
        .isSameAs(annotatedException);
```
Expected: the advice gets out of the way and lets Spring apply the exception's declared HTTP status.

## Local conventions (the project facts the skill omits)
- Package root: `application/src/main/java/com/acme/shop/exceptionhandling/`.
- Naming shape: `RestExceptionHandler` (primary), a narrower
  `RestResponseEntityExceptionHandler` sibling in the same package, plus one capability-local
  `@ControllerAdvice` in `vendor-relation-adapter` for relation-specific exceptions.
- Required collaborators / base types: response body is always
  `ResponseEntity<Map<String, List<String>>>` with key `"errors"` — no problem-details/RFC-7807
  shape in this codebase.
- Config / wiring: plain `@ControllerAdvice` (no `basePackages` scoping) — applies repo-wide.

## Frequency & coverage (why this earned a skill)
- Occurrences: 3 `@ControllerAdvice` files across 2 modules (`application`,
  `vendor-relation-adapter`); 11 `@ExceptionHandler` matches across those 3 files (as of
  `abc1234`).

## Drift / exceptions
- `BindException`/`MethodArgumentNotValidException` are deliberately re-thrown, not translated to
  the `{"errors": [...]}` shape — Spring's own validation-error response format is kept. Not a gap;
  intentional.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@ControllerAdvice'`, `rg '@ExceptionHandler'` (both `--glob '*.java'`)
