---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `resilience4j` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`DefaultRegistryGateway.java`](exemplars/default-registry-gateway.md#resilience4j) | Circuit breaker with a degrade-to-empty fallback | adapter/gateway (`partner-gateway`) |
| [`FulfilmentFhirGateway.java`](exemplars/fulfilment-fhir-gateway.md#resilience4j) | Circuit breaker with a rethrow fallback, shared breaker name across methods | adapter/gateway (`fulfilment-gateway`) |

### Excerpts
Degrade-to-empty fallback (full source in the [registry exemplar](exemplars/default-registry-gateway.md#resilience4j)):
```java
@CircuitBreaker(name = "registry", fallbackMethod = "getRegistryLinksFallback")
public List<RegistryLink> getRegistryLinks(final String nid) {
    ...
}

private List<RegistryLink> getRegistryLinksFallback(final String nid, final Throwable e) {
    log.error("Circuit breaker: Failed to fetch registry links", e);
    return Collections.emptyList();
}
```

Rethrow fallback for a critical dependency:
```java
@CircuitBreaker(name = "fulfilment-fhir", fallbackMethod = "getShipmentsFallback")
public List<Shipment> getShipments(String pseudoAccessToken, String pseudoNid, String storefrontId) {
    ...
}

private List<Shipment> getShipmentsFallback(String pseudoAccessToken, String pseudoNid, String storefrontId, final Throwable e) {
    log.error("Circuit breaker: Failed to fetch shipments from Fulfilment", e);
    throw new FulfilmentException("Unable to get shipments from Fulfilment", e);
}
```

### Edge cases
Lookup failure on a non-critical enrichment call:
```java
when(registryPortType.getCustomerLinks(any())).thenThrow(new RuntimeException("timeout"));

assertThat(gateway.getRegistryLinks("00000000000")).isEmpty();
```
Expected: the fallback returns an empty list and the caller keeps moving.

Primary order lookup fails:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", "CUSTOMER_ORDER_RECORD")
        .willReturn(jsonResponse(503)));

assertThatThrownBy(() -> gateway.getShipments("pseudo-token", "pseudo-nid", "storefront"))
        .isInstanceOf(FulfilmentException.class);
```
Expected: the fallback rethrows because the caller cannot safely continue with silent empties.

Fallback signature drifts from the guarded method:
```java
@CircuitBreaker(name = "registry", fallbackMethod = "brokenFallback")
public List<RegistryLink> getRegistryLinks(final String nid) { ... }
```
Expected: startup or invocation fails loudly; fallback methods must mirror the original parameters plus a trailing `Throwable`.

## Local conventions (the project facts the skill omits)
- Naming shape: `@CircuitBreaker(name = "<integration>", fallbackMethod = "<method>Fallback")` — the
  breaker `name` matches the resilience4j config key for that integration; the fallback method has
  the same signature as the guarded method plus a trailing `Throwable`.
- Required collaborators / base types: fallback is a private method in the same class, never a
  separate bean.
- Config / wiring: per-integration circuit breaker config lives in `application-*.yml` under the
  matching `name`.

## Frequency & coverage (why this earned a skill)
- Occurrences: 9 `@CircuitBreaker` matches across 5 files, 2 modules — `partner-gateway`,
  `fulfilment-gateway` (as of `abc1234`).

## Drift / exceptions
- **Fallback behavior differs by integration criticality**: `DefaultRegistryGateway` degrades to an
  empty list on failure (a non-critical enrichment lookup); `FulfilmentFhirGateway` rethrows a
  `FulfilmentException` (this integration's failure should surface, not be silently swallowed). Pick
  based on whether a caller can tolerate a silent empty result.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@CircuitBreaker' --glob '*.java'`
