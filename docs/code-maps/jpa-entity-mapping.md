---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `jpa-entity-mapping` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`SalesRegionEntity.java`](exemplars/sales-region-entity.md#jpa-entity-mapping) | `@Entity` kept out of the domain, Lombok-built, explicit `@UniqueConstraint` matching a Flyway migration | adapter (`account-adapter`) |

### Excerpts
Minimal entity with explicit uniqueness (full source in the [exemplar leaf](exemplars/sales-region-entity.md#jpa-entity-mapping)):
```java
@Entity
@Table(
        name = "sales_region",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_region_region_code",
                columnNames = {"region", "region_code"}
        )
)
```

JPA-only construction surface:
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SalesRegionEntity {
```

### Edge cases
Attempt to create a duplicate `(region, region_code)` row:
```java
salesRegionRepository.save(SalesRegionEntity.builder().region("CENTRAL").regionCode("00000").build());
salesRegionRepository.save(SalesRegionEntity.builder().region("CENTRAL").regionCode("00000").build());
```
Expected: the database-level unique constraint rejects the duplicate pair.

JPA instantiation path:
```java
var entity = SalesRegionEntity.builder().region("CENTRAL").regionCode("00000").build();
assertThat(entity.getId()).isNull();
```
Expected: application code uses the builder; JPA fills the identifier on persistence.

Reflective framework construction:
```java
var constructor = SalesRegionEntity.class.getDeclaredConstructor();
assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
```
Expected: JPA can still instantiate the entity, while callers outside the package cannot use a public no-args constructor.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. The
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [adapter/jpa-entity](scaffold/adapter/jpa-entity.mustache) | `<capability>-adapter/src/main/java/<pkg>/adapter/repository/<Name>Entity.java` | `package`, `Name`, `table`, `fields[type, name, column]` | nullable columns; relations and fetch types; enums (`@Enumerated`); the migration (a STOP item: get a human decision first) |

## Local conventions (the project facts the skill omits)
- Package root: `<module>/.../repository/<subpackage>/` — entities live alongside the repository
  that maps them, not in a separate `entity` package.
- Naming shape: `*Entity` suffix, separate from both the JPA `*SpringDataJpaRepository` and the
  domain type it is converted to at the boundary.
- Required collaborators / base types: Lombok `@Getter @Builder @NoArgsConstructor(PROTECTED)
  @AllArgsConstructor(PACKAGE)` — no public constructor.
- Config / wiring: `@GeneratedValue(strategy = IDENTITY)` paired with a Postgres `serial` column
  (`columnDefinition = "serial"`).

## Frequency & coverage (why this earned a skill)
- Occurrences: 18 files across 5 modules — `application`, `authentication-adapter`,
  `storefront-adapter`, `account-adapter`, `attribute-mapping-adapter` (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Entity' --glob '*.java'`
