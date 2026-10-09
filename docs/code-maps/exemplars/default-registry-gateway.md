---
runtime: lazy
generated-by: pattern-scanner
source: partner-gateway/src/main/java/com/acme/shop/partner/gateway/registry/DefaultRegistryGateway.java
serves: [gateway-client-hygiene, resilience4j, spring-caching]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../registry/DefaultRegistryGateway.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> Sections are
> anchored so a skill index deep-links only the pattern that concerns it — don't renumber or rename
> an anchor without updating the indexes in `serves:`.

## What this artifact is
The package-private adapter implementing the `RegistryGateway` port for the partner registry SOAP
service (via a generated `RegistryPortType` CXF client). Small (~45 lines) but it demonstrates
three patterns at once: port implementation, resilience, and read-through caching.

## Port implementation {#gateway-client-hygiene}
**Serves:** [`gateway-client-hygiene`](../gateway-client-hygiene.md)

`class DefaultRegistryGateway implements RegistryGateway` — the port (`RegistryGateway`, same
package) is a single-method interface (`List<RegistryLink> getRegistryLinks(String nid)`); the adapter
is package-private (`class`, no `public`), constructor-injected with the generated SOAP client and
a mapper, and wraps the wire type in a domain-shaped return (`List<RegistryLink>`), throwing a
`RegistryException` on a SOAP acknowledgement error rather than leaking the SOAP response type.

## Circuit breaker with fallback {#resilience4j}
**Serves:** [`resilience4j`](../resilience4j.md)

`@CircuitBreaker(name = "registry", fallbackMethod = "getRegistryLinksFallback")` on the gateway
method; the fallback is a private method with the same signature plus a trailing `Throwable`,
returning a safe empty list and logging at `error`. Convention: `name` matches the Resilience4j
config key for this integration, and the fallback never rethrows for a non-critical read.

## Read-through cache {#spring-caching}
**Serves:** [`spring-caching`](../spring-caching.md)

`@Cacheable("registry")` is stacked on the same method, cache name matching the integration. No
explicit key — the single `nid` argument is the implicit key (Spring's default key generator).

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.registry;

import com.acme.shop.partner.registry.core.v2.GetCustomerLinksResponse;
import com.acme.shop.partner.registry.protocol.v2.RegistryPortType;
import com.acme.shop.partner.dto.RegistryLink;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.acme.shop.partner.gateway.registry.RegistryRequestBuilder.buildCustomerLinksRequest;


@Slf4j
@Repository
class DefaultRegistryGateway implements RegistryGateway {
    private final RegistryPortType registryPortType;
    private final RegistryMapper registryMapper;

    public DefaultRegistryGateway(final RegistryPortType registryPortType,
                                  final RegistryMapper registryMapper) {
        this.registryPortType = registryPortType;
        this.registryMapper = registryMapper;
    }

    @Override
    @Cacheable("registry")
    @CircuitBreaker(name = "registry", fallbackMethod = "getRegistryLinksFallback")
    public List<RegistryLink> getRegistryLinks(final String nid) {
        log.debug("Getting registry links");
        final GetCustomerLinksResponse customerLinks =
                registryPortType.getCustomerLinks(buildCustomerLinksRequest(nid));
        if (customerLinks.getAcknowledge() != null && !customerLinks.getAcknowledge().getErrors().isEmpty()) {
            throw new RegistryException(
                    customerLinks.getAcknowledge().getErrors().get(0).getDescription().getValue());
        }
        return registryMapper.mapRegistryLinks(customerLinks.getRegistryList().getEntries());
    }

    private List<RegistryLink> getRegistryLinksFallback(final String nid,
                                                        final Throwable e) {
        log.error("Circuit breaker: Failed to fetch registry links", e);
        return Collections.emptyList();
    }
}
```

### Edge cases
Acknowledgement contains an upstream business error:
```java
when(registryPortType.getCustomerLinks(any())).thenReturn(responseWithError("duplicate customer reference"));

assertThatThrownBy(() -> gateway.getRegistryLinks("00000000000"))
        .isInstanceOf(RegistryException.class);
```
Expected: the gateway throws a domain exception and does not return partially mapped entries.

Repeated lookup for the same customer:
```java
gateway.getRegistryLinks("00000000000");
gateway.getRegistryLinks("00000000000");
```
Expected: the second call is satisfied from the `"registry"` cache.

Temporary upstream outage:
```java
when(registryPortType.getCustomerLinks(any())).thenThrow(new RuntimeException("timeout"));

assertThat(gateway.getRegistryLinks("00000000000")).isEmpty();
```
Expected: the fallback returns an empty list because this lookup is a non-critical enrichment.

## Provenance
- Scanned at: `abc1234` · tool/query: `scripts/scan-patterns.sh --glob '*.java' '@CircuitBreaker' '@Cacheable'` + manual PowerShell module-spread check (see [code-maps/README] for rationale — `dirname` on rg's Windows paths misreports distinct-dir counts, so per-probe module spread was verified directly).

[code-maps/README]: ../README.md
