---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `domain-to-dto-mapper-tests` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`RegistryMapperTest.java`](exemplars/registry-mapper-test.md#domain-to-dto-mapper-tests) | One test per named edge case (not `@ParameterizedTest`), Mockito + manual `@PostConstruct` invocation | gateway / mapper test (`partner-gateway`) |

### Excerpts
Manual lifecycle hook invocation in `@BeforeEach`:
```java
@BeforeEach
public void init() {
    registryMapper.init();
}
```

Descriptive one-test-per-case style:
```java
@Test
void testMapRegistriesUnknownId() {
    unknownRegistry.setValue("unknown-registry");
    assertThat(result.get(0)).isEqualTo(Registry.PAYMENTS);
}
```

### Edge cases
Mapper depends on state built in `@PostConstruct`:
```java
@InjectMocks
private RegistryMapper registryMapper;

@BeforeEach
public void init() {
    registryMapper.init();
}
```
Expected: tests initialize the lookup table explicitly because Mockito does not call lifecycle methods.

Empty identifier list case stays isolated in its own test:
```java
input.get(0).getIds().clear();
var result = registryMapper.mapRegistries(input);
assertThat(result).hasSize(1);
```
Expected: a failing edge case names itself clearly, without needing to inspect a parameterized input table.

Unknown id case verifies drop-on-unknown behavior:
```java
unknownRegistry.setValue("unknown-registry");
var result = registryMapper.mapRegistries(input);
```
Expected: the test proves the mapper ignores unknown ids instead of creating a partial domain value.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. The
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [adapter/to-dto-mapper-test](../Moustache%20scripts/adapter/to-dto-mapper-test.mustache) | `<capability>-adapter/src/test/java/<pkg>/adapter/controller/<Domain>ToDTOMapperTest.java` | `package`, `Domain`, `fixture`, `fields[name, example]` (`example` must equal the Mother's default) | parameterized null, empty and enum cases; the round-trip if a reverse mapper exists |

## Local conventions (the project facts the skill omits)
- Package root: same package as the mapper under test.
- Naming shape: `*MapperTest`, with descriptive `test<Scenario>` method names rather than
  parameterized cases.
- Required collaborators / base types: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`;
  `@BeforeEach` manually invokes any `@PostConstruct` init the mapper relies on.
- Config / wiring: none — pure unit test.

## Frequency & coverage (why this earned a skill)
- Occurrences: 67 `*MapperTest` files across 14 modules (as of `abc1234`), against 171 `*Mapper`
  production classes.

## Drift / exceptions
- Not every `*Mapper` has a dedicated test file 1:1 — some are exercised only indirectly via their
  caller's tests.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*MapperTest\b' --glob '*.java'`
