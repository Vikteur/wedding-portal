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
# Code map — `junit-tests` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> This map mirrors [jvm-testing.md](jvm-testing.md) because the repo treats JUnit 5 conventions and JVM-testing as one bundle.

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`GetInvoicesUseCaseTest.java`](exemplars/get-invoices-usecase-test.md#junit-tests) | Canonical JUnit 5 anatomy: `@ExtendWith(MockitoExtension.class)`, AssertJ assertions, parameterized enum coverage | usecase test (`invoice-usecase`) |

### Excerpts
JUnit 5 + Mockito wiring only:
```java
@ExtendWith(MockitoExtension.class)
class GetInvoicesUseCaseTest {
    @Mock private InvoicesRepository invoicesRepository;
    @InjectMocks private GetInvoicesUseCase getInvoicesUseCase;
}
```

Parameterized coverage for an enum-backed rule:
```java
@ParameterizedTest
@EnumSource(value = InvoiceDocumentType.class, names = {
        "CAMPAIGN_SUMMARY",
        "SUPPLY_PLAN",
        "ACTIVITY_NOTE",
        "CATALOG_SNAPSHOT",
        "ORDER_CONFIRMATION",
        "WAREHOUSE_PLAN"
})
void givenInvoicesWithOnlyExcludedDocumentTypes_whenGetInvoices_thenReturnEmptyList(InvoiceDocumentType documentType) {
```

### Edge cases
Shared setup for every test method:
```java
@BeforeEach
void setUp() {
    when(authenticationFacade.getAuthentication()).thenReturn(customer);
}
```
Expected: each test starts from a consistent principal and only overrides what its scenario needs.

Feature-disabled branch uses interaction assertions:
```java
when(featureFlagUseCase.execute()).thenReturn(List.of());
verifyNoInteractions(invoiceCollectionEnricher);
```
Expected: the assertion focuses on observable contract, not implementation details.

Single excluded invoice type through a parameterized case:
```java
var excludedInvoice = excludedInvoice(documentType);
assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));
```
Expected: one test shape covers every excluded enum constant without repeated boilerplate.

## Local conventions (the project facts the skill omits)
- JUnit 5 is the default unit-test runner, paired with Mockito and AssertJ.
- Given/When/Then intent lives in method names instead of `@DisplayName` or comment blocks.
- `@ParameterizedTest` is preferred when the same assertion shape must cover a closed enum set.
- Tests stay outside Spring unless they are deliberately slice or integration tests.

## Frequency & coverage (why this earned a skill)
- JUnit 5 is present across the unit-test corpus; the same repository conventions described in
  [jvm-testing.md](jvm-testing.md) apply here as well.

## Drift / exceptions
- Some legacy tests still use plain `@Test` loops where a parameterized form would be clearer.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@ExtendWith\(MockitoExtension.class\)|@ParameterizedTest' --glob '*Test.java'`
