---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `spring-boot-slice-tests` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`AbstractControllerTest.java`](exemplars/abstract-controller-test.md#spring-boot-slice-tests) | `@WebMvcTest` base with a narrowed `@ComponentScan`, shared `@MockitoBean`s | adapter test (`account-adapter`) |

### Excerpts
Slice boundary narrowed to controllers only:
```java
@WebMvcTest
@ComponentScan(
        basePackages = "com.acme.shop.account.adapter.controller",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*Controller"
        )
)
```

Shared collaborators live in the abstract base class:
```java
@MockitoBean
protected GetDelegationsUseCase getDelegationsUseCase;

@MockitoBean
protected GetCustomerInformationUseCase getCustomerInformationUseCase;
```

### Edge cases
Non-controller beans sit in the same package tree:
```java
includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Controller")
```
Expected: helper beans outside the controller suffix stay out of the slice and must be mocked explicitly.

A controller collaborator is forgotten:
```java
@MockitoBean
protected VerifyDelegationForTopicAccessUseCase verifyDelegationForTopicAccessUseCase;
```
Expected: the shared base declares every required collaborator once so concrete tests do not fail with unsatisfied-bean noise.

Concrete subclass needs MockMvc only:
```java
@Autowired
protected MockMvc mockMvc;
```
Expected: subclasses inherit the slice configuration and focus on request/response assertions.

## Local conventions (the project facts the skill omits)
- Package root: `<module>-adapter/src/test/java/.../controller/` for `@WebMvcTest`;
  `.../repository/` for `@DataJpaTest`.
- Naming shape: a shared `Abstract*Test` per adapter module holding the slice annotation + common
  `@MockitoBean`s, with concrete `*ControllerTest` / `*RepositoryTest` classes extending it.
- Required collaborators / base types: `@ComponentScan(useDefaultFilters = false, includeFilters =
  @Filter(type = REGEX, pattern = ".*Controller"))` to scope the slice to just the controller
  package.
- Config / wiring: `@ContextConfiguration(classes = ...TestConfig.class)` for a minimal test config.

## Frequency & coverage (why this earned a skill)
- Occurrences: `@WebMvcTest` — 19 files / 15 modules; `@SpringBootTest` — 15 files / 6 modules;
  `@DataJpaTest` — 6 files / 4 modules (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@WebMvcTest'`, `rg '@SpringBootTest'`, `rg '@DataJpaTest'` (all `--glob '*.java'`)
