---
runtime: lazy
source: account-adapter/src/main/java/com/acme/shop/account/adapter/repository/customer/SalesRegionEntity.java
serves: [jpa-entity-mapping]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `account-adapter/.../repository/customer/SalesRegionEntity.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A small, clean `@Entity` (plain Jakarta Persistence, not `PanacheEntity`) — no domain logic, kept separate from the `SalesRegion` domain enum it is
mapped to and from at the repository edge (see [sales-region-jpa-repository]).

## `@Entity` kept out of the domain {#jpa-entity-mapping}
**Serves:** [`jpa-entity-mapping`](../jpa-entity-mapping.md)

Lives under the adapter's `repository.*` package, never imported by `*-domain` or `*-usecase` code.
Lombok `@Getter`/`@Builder`/`@NoArgsConstructor(PROTECTED)`/`@AllArgsConstructor(PACKAGE)` — no
public no-args or all-args constructor, so instances are only built via the builder or Hibernate
reflection. `@Table` declares an explicit `@UniqueConstraint` with a named constraint (matches the
Flyway migration that created it, see [topics-delegation-migration] for the migration
convention). `@GeneratedValue(strategy = IDENTITY)` with an explicit `columnDefinition = "serial"` —
local convention pairing a Postgres `serial` column with `IDENTITY` generation rather than a
sequence.

### Source (pseudonymized)
```java
package com.acme.shop.account.adapter.repository.customer;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@Entity
@Table(
        name = "sales_region",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_region_region_code",
                columnNames = {"region", "region_code"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SalesRegionEntity {

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "region_code")
    private String regionCode;

    @Column(nullable = false, name = "region")
    private String region;
}
```

### Edge cases
Builder-created transient entity:
```java
var entity = SalesRegionEntity.builder()
        .region("CENTRAL")
        .regionCode("00000")
        .build();
assertThat(entity.getId()).isNull();
```
Expected: application code can prepare an entity without an identifier; JPA assigns the key on persistence.

Duplicate logical row:
```java
salesRegionPanacheRepository.persistAndFlush(SalesRegionEntity.builder().region("CENTRAL").regionCode("00000").build());
salesRegionPanacheRepository.persistAndFlush(SalesRegionEntity.builder().region("CENTRAL").regionCode("00000").build());
```
Expected: the `unique_region_region_code` constraint rejects the second row.

Framework-only no-args constructor:
```java
var constructor = SalesRegionEntity.class.getDeclaredConstructor();
assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
```
Expected: frameworks can instantiate the entity reflectively, while production code avoids a public empty constructor.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Entity' --glob '*.java'` (18 files / 5 modules)

[sales-region-jpa-repository]: sales-region-jpa-repository.md
[topics-delegation-migration]: topics-delegation-migration.md
