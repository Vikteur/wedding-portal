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
# Code map — `persistence-repository` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`SalesRegionJpaRepository.java`](exemplars/sales-region-jpa-repository.md#persistence-repository) | Port-implementing adapter delegating to a `JpaRepository`, entity→domain mapping with a safe default | adapter (`account-adapter`) |

### Excerpts
Thin adapter over Spring Data JPA (full source in the [exemplar leaf](exemplars/sales-region-jpa-repository.md#persistence-repository)):
```java
public class SalesRegionJpaRepository implements SalesRegionRepository {

    private final SalesRegionSpringDataJpaRepository jpaRepository;
```

Boundary mapping plus cache:
```java
@Override
@Cacheable("salesRegion")
public SalesRegion findByRegionCode(String regionCode) {
    return jpaRepository.findByRegionCode(regionCode)
            .map(entity -> SalesRegion.valueOf(entity.getRegion()))
            .orElse(SalesRegion.CENTRAL);
}
```

### Edge cases
No row found for the lookup key:
```java
when(jpaRepository.findByRegionCode("00000")).thenReturn(Optional.empty());
assertThat(repository.findByRegionCode("00000")).isEqualTo(SalesRegion.CENTRAL);
```
Expected: the adapter returns the agreed safe default instead of throwing.

Repeated lookup for the same code:
```java
repository.findByRegionCode("00000");
repository.findByRegionCode("00000");
```
Expected: Spring caching can satisfy the second read from cache because the port method is the cache boundary.

Unexpected enum value in persisted data:
```java
when(jpaRepository.findByRegionCode("00000"))
        .thenReturn(Optional.of(SalesRegionEntity.builder().region("UNKNOWN").regionCode("00000").build()));
```
Expected: any invalid persisted value fails at the adapter boundary rather than leaking a JPA entity downstream.

## Local conventions (the project facts the skill omits)
- Package root: port interface (`*Repository`) in the `*-usecase` or `*-domain` module; the
  Spring Data adapter (`*JpaRepository` implementing the port, plus a
  `*SpringDataJpaRepository extends JpaRepository<...>`) in the matching `*-adapter` module.
- Naming shape: port `<Thing>Repository`; adapter `<Thing>JpaRepository`; the raw Spring Data
  interface `<Thing>SpringDataJpaRepository`.
- Required collaborators / base types: adapter converts the JPA entity to the domain value at the
  boundary — never returns a JPA entity from a port method.
- Config / wiring: `@Repository` stereotype on the adapter.

## Frequency & coverage (why this earned a skill)
- Occurrences: 50 `interface *Repository` files across 18 modules (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'interface[[:space:]]+[A-Za-z]*Repository' --glob '*.java'`
