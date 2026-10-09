---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `persistence-repository` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`DefaultSalesRegionRepository.java`](exemplars/sales-region-jpa-repository.md#persistence-repository) | Port-implementing adapter delegating to a `PanacheRepository`, entity→domain mapping with a safe default | adapter (`account-adapter`) |

### Excerpts
Thin adapter over Hibernate ORM with Panache (full source in the [exemplar leaf](exemplars/sales-region-jpa-repository.md#persistence-repository)):
```java
public class DefaultSalesRegionRepository implements SalesRegionRepository {

    private final SalesRegionPanacheRepository panacheRepository;
```

Boundary mapping plus cache:
```java
@Override
@CacheResult(cacheName = "salesRegion")
public SalesRegion findByRegionCode(String regionCode) {
    return panacheRepository.findByRegionCode(regionCode)
            .map(entity -> SalesRegion.valueOf(entity.getRegion()))
            .orElse(SalesRegion.CENTRAL);
}
```

### Edge cases
No row found for the lookup key:
```java
when(panacheRepository.findByRegionCode("00000")).thenReturn(Optional.empty());
assertThat(repository.findByRegionCode("00000")).isEqualTo(SalesRegion.CENTRAL);
```
Expected: the adapter returns the agreed safe default instead of throwing.

Repeated lookup for the same code:
```java
repository.findByRegionCode("00000");
repository.findByRegionCode("00000");
```
Expected: Quarkus cache (`@CacheResult`) can satisfy the second read from cache because the port method is the cache boundary.

Unexpected enum value in persisted data:
```java
when(panacheRepository.findByRegionCode("00000"))
        .thenReturn(Optional.of(SalesRegionEntity.builder().region("UNKNOWN").regionCode("00000").build()));
```
Expected: any invalid persisted value fails at the adapter boundary rather than leaking a JPA entity downstream.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. Each
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [adapter/panache-repository](scaffold/adapter/panache-repository.mustache) | `<capability>-adapter/src/main/java/<pkg>/adapter/repository/<Name>PanacheRepository.java` | `package`, `Name`, `Key`, `key`, `keyType` | other query methods |
| [adapter/default-repository](scaffold/adapter/default-repository.mustache) | `.../adapter/repository/Default<Name>Repository.java` | `package`, `Name`, `Key`, `key`, `keyType`, `fields[name, pascal]` | writes (`persist`); other port methods; nested or collection mapping; a port that is not `Optional<Name> findBy<Key>` |

The base case is one `findBy<Key>` lookup. The port comes from `usecase-orchestration` (repository-port), the entity
from `jpa-entity-mapping`, the test from `jvm-testing` (default-repository-test). The templates follow the skill's
Quarkus Panache shape.

## Local conventions (the project facts the skill omits)
- Package root: port interface (`*Repository`) in the `*-usecase` or `*-domain` module; the
  Panache adapter (`Default*Repository` implementing the port, plus a package-private
  `*PanacheRepository implements PanacheRepository<...>`) in the matching `*-adapter` module.
- Naming shape: port `<Thing>Repository`; adapter `Default<Thing>Repository`; the raw Panache
  repository `<Thing>PanacheRepository`.
- Required collaborators / base types: adapter converts the JPA entity to the domain value at the
  boundary — never returns a JPA entity from a port method.
- Config / wiring: `@ApplicationScoped` + Lombok `@RequiredArgsConstructor` on the adapter;
  `@ApplicationScoped` on the package-private Panache repository.

## Frequency & coverage (why this earned a skill)
- Occurrences: 50 `interface *Repository` files across 18 modules (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'interface[[:space:]]+[A-Za-z]*Repository' --glob '*.java'`
