---
runtime: lazy
source: partner-gateway/src/main/java/com/acme/shop/partner/gateway/registry/DefaultRegistryGateway.java
serves: [resilience4j]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../registry/DefaultRegistryGateway.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> Sections are
> anchored so a skill index deep-links only the pattern that concerns it — don't renumber or rename
> an anchor without updating the indexes in `serves:`.

## What this artifact is
The adapter for the partner registry SOAP service (via a generated `RegistryPortType` Quarkus CXF
client, injected with `@CXFClient("registry")`).
It demonstrates a SmallRye Fault Tolerance circuit breaker with a degrade-to-empty fallback.

## Circuit breaker with fallback {#resilience4j}
**Serves:** [`resilience4j`](../resilience4j.md)

`@CircuitBreaker` plus `@Fallback(fallbackMethod = "getRegistryLinksFallback")` on the gateway
method; the fallback is a private method with exactly the same signature, returning a safe empty
list and logging at `error`. Convention: the breaker is identified by
`com.acme.shop.partner.gateway.registry.DefaultRegistryGateway/getRegistryLinks`, which is also its
config key, and the fallback never rethrows for a non-critical read.

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.registry;

import com.acme.shop.partner.registry.core.v2.GetCustomerLinksResponse;
import com.acme.shop.partner.registry.protocol.v2.RegistryPortType;
import com.acme.shop.partner.dto.RegistryLink;
import io.quarkiverse.cxf.annotation.CXFClient;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import java.util.Collections;
import java.util.List;

import static com.acme.shop.partner.gateway.registry.RegistryRequestBuilder.buildCustomerLinksRequest;


@Slf4j
@ApplicationScoped
class DefaultRegistryGateway implements RegistryGateway {
    private final RegistryPortType registryPortType;
    private final RegistryMapper registryMapper;

    public DefaultRegistryGateway(@CXFClient("registry") final RegistryPortType registryPortType,
                                  final RegistryMapper registryMapper) {
        this.registryPortType = registryPortType;
        this.registryMapper = registryMapper;
    }

    @Override
    @CircuitBreaker
    @Fallback(fallbackMethod = "getRegistryLinksFallback")
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

    private List<RegistryLink> getRegistryLinksFallback(final String nid) {
        log.error("Circuit breaker: Failed to fetch registry links");
        return Collections.emptyList();
    }
}
```

```properties
quarkus.fault-tolerance."com.acme.shop.partner.gateway.registry.DefaultRegistryGateway/getRegistryLinks".circuit-breaker.request-volume-threshold=10
```

### Edge cases
Acknowledgement contains an upstream business error:
```java
when(registryPortType.getCustomerLinks(any())).thenReturn(responseWithError("duplicate customer reference"));

assertThatThrownBy(() -> gateway.getRegistryLinks("00000000000"))
        .isInstanceOf(RegistryException.class);
```
Expected: the gateway throws a domain exception and does not return partially mapped entries.

Temporary upstream outage:
```java
when(registryPortType.getCustomerLinks(any())).thenThrow(new RuntimeException("timeout"));

assertThat(gateway.getRegistryLinks("00000000000")).isEmpty();
```
Expected: the fallback returns an empty list because this lookup is a non-critical enrichment.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@CircuitBreaker' --glob '*.java'`.

[code-maps/README]: ../README.md
