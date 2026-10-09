---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `resilience4j` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`DefaultRegistryGateway.java`](exemplars/default-registry-gateway.md#resilience4j) | SmallRye Fault Tolerance circuit breaker with a degrade-to-empty fallback | adapter/gateway (`partner-gateway`) |

### Excerpts
Degrade-to-empty fallback (full source in the [registry exemplar](exemplars/default-registry-gateway.md#resilience4j)):
```java
@CircuitBreaker
@Fallback(fallbackMethod = "getRegistryLinksFallback")
public List<RegistryLink> getRegistryLinks(final String nid) {
    ...
}

private List<RegistryLink> getRegistryLinksFallback(final String nid) {
    log.error("Circuit breaker: Failed to fetch registry links");
    return Collections.emptyList();
}
```

### Edge cases
Lookup failure on a non-critical enrichment call:
```java
when(registryPortType.getCustomerLinks(any())).thenThrow(new RuntimeException("timeout"));

assertThat(gateway.getRegistryLinks("00000000000")).isEmpty();
```
Expected: the fallback returns an empty list and the caller keeps moving.

Fallback signature drifts from the guarded method:
```java
@CircuitBreaker
@Fallback(fallbackMethod = "brokenFallback")
public List<RegistryLink> getRegistryLinks(final String nid) { ... }
```
Expected: deployment fails loudly at build/startup (fault tolerance definition validation); fallback
methods must have exactly the same parameters and return type as the guarded method.

## Local conventions (the project facts the skill omits)
- Naming shape: `@CircuitBreaker` + `@Fallback(fallbackMethod = "<method>Fallback")` — the breaker
  is identified by `<fully-qualified class>/<method>` (no `name` string); the fallback method has
  the same signature as the guarded method.
- Required collaborators / base types: fallback is a private method in the same `@ApplicationScoped`
  gateway, never a separate bean or `FallbackHandler` class.
- Config / wiring: per-integration overrides live in `application.properties` under
  `quarkus.fault-tolerance."<FQCN>/<method>".circuit-breaker.*` (profile-specific with `%dev.`/`%prod.`).

## Frequency & coverage (why this earned a skill)
- Occurrences: 9 `@CircuitBreaker` matches across 5 files, 2 modules — `partner-gateway`,
  `fulfilment-gateway` (as of `abc1234`).

## Drift / exceptions
- **Fallback behavior follows integration criticality**: this non-critical enrichment lookup
  degrades to an empty list. Critical dependencies should surface failure rather than silently
  returning an empty result.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@CircuitBreaker' --glob '*.java'`
