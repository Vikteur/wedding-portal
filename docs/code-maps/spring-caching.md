---
runtime: lazy
generated-by: pattern-scanner
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `spring-caching` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`DefaultRegistryGateway.java`](exemplars/default-registry-gateway.md#spring-caching) | `@Cacheable` stacked with `@CircuitBreaker` on a gateway method | adapter/gateway (`partner-gateway`) |

### Excerpts
Gateway cache with the integration name as cache key space (full source in the [exemplar leaf](exemplars/default-registry-gateway.md#spring-caching)):
```java
@Override
@Cacheable("registry")
@CircuitBreaker(name = "registry", fallbackMethod = "getRegistryLinksFallback")
public List<RegistryLink> getRegistryLinks(final String nid) {
    ...
}
```

### Edge cases
Same customer identifier requested twice:
```java
gateway.getRegistryLinks("00000000000");
gateway.getRegistryLinks("00000000000");
```
Expected: the second call is served from the `"registry"` cache and does not hit the upstream client again.

Different identifiers:
```java
gateway.getRegistryLinks("00000000000");
gateway.getRegistryLinks("11111111111");
```
Expected: Spring keeps separate cache entries because the full argument list is the implicit key.

Circuit breaker fallback after a cached success:
```java
when(registryPortType.getCustomerLinks(any())).thenReturn(successResponse());
gateway.getRegistryLinks("00000000000");
when(registryPortType.getCustomerLinks(any())).thenThrow(new RuntimeException("timeout"));
gateway.getRegistryLinks("00000000000");
```
Expected: the cached value wins; the fallback is not needed while the entry remains warm.

## Local conventions (the project facts the skill omits)
- Naming shape: cache name string matches the integration/entity name (`"registry"`,
  `"salesRegion"`) — no shared/generic cache names.
- Required collaborators / base types: no explicit `@CacheConfig` seen at class level in the
  exemplars; cache name is repeated per `@Cacheable` call site.
- Config / wiring: cache managers/TTLs are configured per module (for example
  `SalesRegionCacheConfig`, `FaqCacheConfig`, `TranslationsCacheConfig`) — a dedicated
  `*CacheConfig` `@Configuration` class per cached resource.

## Frequency & coverage (why this earned a skill)
- Occurrences: 13 `@Cacheable` matches across 11 files, 8 modules (as of `abc1234`):
  `access-matrix-adapter`, `authentication-adapter`, `payments-gateway`, `storefront-adapter`,
  `partner-gateway`, `account-adapter`, `attribute-mapping-adapter`, `loyalty-gateway`.

## Drift / exceptions
- None observed — no explicit key generators or conditional caching (`unless`/`condition`) in the
  sampled exemplars; simple whole-argument-list keying throughout.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Cacheable' --glob '*.java'`
