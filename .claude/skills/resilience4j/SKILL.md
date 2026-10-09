---
name: resilience4j
description: SmallRye Fault Tolerance timeouts, retries, circuit breakers and fallbacks on outbound calls. Use for any gateway or remote call.
---
# Fault tolerance — timeouts, retries, circuit breakers, fallbacks

> **Generic "how" only.** No service names, endpoints, or instance names in the body — those live in
> the code-map leaf.

## When to use
Adding fault tolerance to an outbound call (HTTP/SOAP/FHIR client) so a slow or failing dependency
degrades gracefully instead of cascading: timeout, retry, circuit breaker, fallback.

## How
- Annotate the gateway method with the MicroProfile Fault Tolerance annotations that SmallRye
  implements: `@Timeout`, `@Retry`, `@CircuitBreaker` and `@Fallback(fallbackMethod = "<fallback>")`.
  The fallback must share the method signature (or use a `FallbackHandler`).
- Set each annotation's parameters per gateway (failure ratio, request volume threshold, delay,
  retry count and delay). One configuration per dependency, not one global.
- Override values per environment through configuration properties
  (`<class>/<method>/CircuitBreaker/delay`), not by editing code.
- Make the fallback meaningful: a cached/empty/typed-degraded result, or a domain exception that maps
  to a clear HTTP status — never swallow the error silently.
- Expose the breaker state through the health and metrics endpoints; keep those endpoints authenticated.

## Pattern signals (discovery cues)
`quarkus-smallrye-fault-tolerance` on the classpath; `@Timeout` / `@Retry` / `@CircuitBreaker` /
`@Fallback` annotations on gateway/client classes; `fallbackMethod = ` references; per-method
override keys in the configuration.

## Project specifics → see docs
- Code map (configured instances, thresholds, fallbacks, exemplars) → `docs/code-maps/resilience4j.md` *(per-repo map, written by the pattern-scanner — resolves once harvested)*

## Guardrails (what NOT to do)
- Don't share one breaker configuration across unrelated dependencies — tune per integration.
- Don't let a fallback hide a bug: log it, and don't return success-shaped data for a hard failure.
- Don't put a circuit breaker on a fast in-process call — it's for remote/IO boundaries.

## Definition of done
- [ ] Each outbound dependency has its own timeout, retry and breaker settings + a typed fallback; thresholds configured; health
  exposed; a test proves the fallback path fires when the breaker is open.
