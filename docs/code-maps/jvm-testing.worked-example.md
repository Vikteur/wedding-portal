---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `jvm-testing` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> This map also serves the `junit-tests` skill — the two describe one convention set here.

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`GetInvoicesUseCaseTest.java`](exemplars/get-invoices-usecase-test.md#jvm-testing) | Behavior-spec unit test: GWT method naming, AssertJ, Mockito at ports, Object Mothers for values, `@ParameterizedTest` for enum cases | usecase (`invoice-usecase`) |

### Excerpts
Port-level mocking with value objects supplied by Mothers:
```java
@Mock
private InvoicesRepository invoicesRepository;

@Mock
private AuthenticationFacade authenticationFacade;

@InjectMocks
private GetInvoicesUseCase getInvoicesUseCase;
```

AssertJ + Mockito stay at the observable boundary:
```java
assertThat(getInvoicesUseCase.execute())
        .isEqualTo(new InvoiceInfo(List.of(invoice), false, List.of()));
verifyNoInteractions(invoiceCollectionEnricher);
```

### Edge cases
Feature disabled path:
```java
when(featureFlagUseCase.execute()).thenReturn(List.of());
assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(invoice), false, List.of()));
verifyNoInteractions(invoiceCollectionEnricher);
```
Expected: the unit test proves observable behavior without starting Spring.

Excluded document types stay filtered:
```java
var excludedInvoice = excludedInvoice(InvoiceDocumentType.CATALOG_SNAPSHOT);
assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));
```
Expected: filtering is asserted through return values, not by peeking into private helpers.

Collection enrichment is opt-in and happens after filtering:
```java
verify(invoiceCollectionEnricher).enrich(List.of(regularInvoice));
```
Expected: mocks sit at ports; domain and DTO values are real instances.

## Local conventions (the project facts the skill omits)
- Given/When/Then lives in the method name, not in comments: names spell the whole scenario as
  `given<State>_when<Action>_then<Outcome>`.
- The mocking boundary is the port: gateways and repository interfaces get `@Mock`; the use case
  under test gets `@InjectMocks`; domain/value objects are built with Object Mothers (see
  [object-mother-builders]) and never mocked.
- Assertions: AssertJ `assertThat`; plain JUnit assertions are the exception.
- Repetition: `@ParameterizedTest` (+ `@EnumSource` for exhaustive enum coverage) and occasional
  `@Nested` grouping when it helps readability.
- Fast units: plain JUnit 5 + Mockito + AssertJ; anything needing Spring context or containers
  moves to the integration-test tier.
- Test sources sit in `<module>/src/test/java`, mirroring the production package.

## Frequency & coverage (why this earned a skill)
- 413 `*Test.java` files across every capability triad (as of `abc1234`); the GWT naming shape
  covers 94% of them.

## Drift / exceptions
- A small set of legacy tests still carries `// given`-style comments instead of keeping the full
  structure purely in the method name.

## Provenance
- Scanned at: `abc1234` · tool/query: `grep -rl 'void given.*_when.*_then' --include='*Test.java'`

[object-mother-builders]: object-mother-builders.md
