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
