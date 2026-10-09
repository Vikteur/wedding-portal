---
runtime: lazy
source: partner-gateway/src/main/java/com/acme/shop/partner/gateway/registry/RegistryMapper.java
serves: [domain-dto-mapper]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../registry/RegistryMapper.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A CDI `@ApplicationScoped` mapper converting the wire shape (`List<PartnerRecordType>`) to the domain enum
`Registry`, at the gateway edge.

## Wire-to-domain mapper {#domain-dto-mapper}
**Serves:** [`domain-dto-mapper`](../domain-dto-mapper.md)

`public class RegistryMapper` exposes one public method `mapRegistries(List<PartnerRecordType>)`.
The local convention is a static lookup table (`Map<String, Registry>`) built once in a
`@PostConstruct init()` method rather than a switch — field-explicit, null / empty-safe, and never
throwing on an unknown id (it silently drops it). No framework type leaks past this class.

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.registry;

import com.acme.partner.standards.catalog.schema.v1.PartnerRecordType;
import com.acme.shop.partner.dto.Registry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class RegistryMapper {

    private Map<String, Registry> registries;

    @PostConstruct
    protected void init() {
        this.registries = new HashMap<>();
        this.registries.put("1000000001", Registry.NORTH);
        this.registries.put("1000000002", Registry.LOYALTY);
        this.registries.put("1000000003", Registry.PAYMENTS);
        this.registries.put("1000000004", Registry.SOUTH);
        this.registries.put("1000000005", Registry.FULFILMENT);
    }

    public List<Registry> mapRegistries(final List<PartnerRecordType> registryList) {
        return registryList.stream()
                .map(partnerRecordType -> partnerRecordType.getIds().isEmpty()
                        || partnerRecordType.getIds().get(0) == null
                        || partnerRecordType.getIds().get(0).getValue() == null ? null : partnerRecordType.getIds().get(0).getValue())
                .filter(Objects::nonNull)
                .map(id -> registries.get(id))
                .filter(Objects::nonNull)
                .toList();
    }
}
```

### Edge cases
Incoming partner record has no ids:
```java
partnerRecordType.getIds().isEmpty() ? null : partnerRecordType.getIds().get(0).getValue()
```
Expected: the mapper yields no domain enum for that entry and keeps handling the rest.

Incoming id object is present but has no value:
```java
partnerRecordType.getIds().get(0).getValue() == null ? null : partnerRecordType.getIds().get(0).getValue()
```
Expected: the pipeline filters the entry out before the lookup map is consulted.

Unknown registry code:
```java
.map(id -> registries.get(id))
.filter(Objects::nonNull)
```
Expected: unmapped ids disappear cleanly without fallback placeholders.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*Mapper' --glob '*.java'`
