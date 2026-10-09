---
name: exception-to-http
description: Map a domain exception hierarchy to HTTP status via one class of exception mappers. Use when surfacing REST errors.
---
# Exception-to-HTTP mapping

## When to use
Turning typed domain/gateway exceptions into consistent HTTP responses (status + error body) without
try/catch scattered through resources.

## How
- Define a small exception hierarchy (not-found, validation/conflict, upstream-unavailable, forbidden);
  throw the typed exception from the inner layers — resources don't catch.
- Centralize translation in one class of `@ServerExceptionMapper` methods; each method maps an
  exception (family) to a `Response` with a status + a uniform error body. Map the **base** types,
  with overrides for specifics. Don't scatter separate `ExceptionMapper` provider classes.
- Keep the error body shape consistent (code, message, optional details); never leak stack traces or
  internal identifiers.
- Default unmapped exceptions to 500 with a generic body, and log the detail server-side only.

## Pattern signals (discovery cues)
A class with `@ServerExceptionMapper` methods (or `ExceptionMapper<T>` providers); a domain
exception base class hierarchy; `Response` error bodies.

## Project specifics → see docs
- Code map (the exception hierarchy → status table, mapper exemplar) → `docs/code-maps/exception-to-http.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't catch exceptions in resources — let the mapper handle them.
- Don't leak stack traces, internal ids, or upstream messages to clients.
- Don't return 200 with an error payload — use the right status.

## Definition of done
- [ ] Typed exceptions thrown inward; one central mapper class maps families → status + uniform body; unmapped → 500 logged server-side; a test asserts the mapping.
