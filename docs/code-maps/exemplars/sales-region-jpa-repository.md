---
runtime: lazy
generated-by: pattern-scanner
source: account-adapter/src/main/java/com/acme/shop/account/adapter/repository/customer/SalesRegionJpaRepository.java
serves: [persistence-repository, spring-caching]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../repository/customer/SalesRegionJpaRepository.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A thin adapter implementing a domain repository port over Spring Data JPA — the standard shape for
the repo's `*Repository` implementations, and small enough to also show a cached read in the same class.

## Port-implementing repository adapter {#persistence-repository}
**Serves:** [`persistence-repository`](../persistence-repository.md)

`public class SalesRegionJpaRepository implements SalesRegionRepository` (the port lives
in the `*-usecase` or `*-domain` module, this adapter in `*-adapter`) — delegates to a
`SalesRegionSpringDataJpaRepository` (the actual `JpaRepository<...>`, see
[sales-region-entity] for the `@Entity` it maps), converting the JPA entity to the
domain value at the boundary (`SalesRegion.valueOf(entity.getRegion())`) with a safe default
(`.orElse(SalesRegion.CENTRAL)`) rather than throwing when no row is found.

## Cached repository read {#spring-caching}
**Serves:** [`spring-caching`](../spring-caching.md)

`@Cacheable("salesRegion")` on the port method — same convention as
[default-registry-gateway] — cache name matches the integration, implicit single-arg key.

### Source (pseudonymized)
```java
package com.acme.shop.account.adapter.repository.customer;

import com.acme.shop.account.domain.customer.SalesRegion;
import com.acme.shop.account.repository.SalesRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SalesRegionJpaRepository implements SalesRegionRepository {

    private final SalesRegionSpringDataJpaRepository jpaRepository;

    @Override
    @Cacheable("salesRegion")
    public SalesRegion findByRegionCode(String regionCode) {
        return jpaRepository.findByRegionCode(regionCode)
                .map(entity -> SalesRegion.valueOf(entity.getRegion()))
                .orElse(SalesRegion.CENTRAL);
    }
}
```

### Edge cases
Unknown region code:
```java
when(jpaRepository.findByRegionCode("00000")).thenReturn(Optional.empty());
assertThat(repository.findByRegionCode("00000")).isEqualTo(SalesRegion.CENTRAL);
```
Expected: the adapter returns the agreed safe default instead of propagating an empty optional into the domain API.

Repeated lookups:
```java
repository.findByRegionCode("00000");
repository.findByRegionCode("00000");
```
Expected: Spring caching can serve the second call from the `salesRegion` cache.

Persisted enum drift:
```java
when(jpaRepository.findByRegionCode("00000"))
        .thenReturn(Optional.of(SalesRegionEntity.builder().region("UNKNOWN").regionCode("00000").build()));
```
Expected: invalid persisted data fails at the adapter boundary instead of leaking a mismatched string deeper into the app.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'interface[[:space:]]+[A-Za-z]*Repository' --glob '*.java'` (50 files / 18 modules), `rg '@Cacheable'` (13 matches / 11 files / 8 modules)

[sales-region-entity]: sales-region-entity.md
[default-registry-gateway]: default-registry-gateway.md
