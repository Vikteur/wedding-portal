---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `usecase-bdd-spec` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`GetInvoicesUseCaseTest.java`](exemplars/get-invoices-usecase-test.md#usecase-bdd-spec) | `given..._when..._then...` method naming, Mockito + AssertJ, `@ParameterizedTest` / `@EnumSource` for exhaustive cases | usecase test (`invoice-usecase`) |

### Excerpts
BDD sentence in the method name:
```java
@Test
void givenAuthenticatedCustomerAndNoInvoicesAndAccountIsActiveAndNoResultsAreUnavailableFromPartners_whenGetInvoices_thenReturnEmptyList() {
    when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(), false, List.of()));
    assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));
}
```

Exhaustive enum coverage without copy-pasting six tests:
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
No invoices, active account:
```java
when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(), false, List.of()));
```
Expected: the test name already states the full outcome; the body only needs minimal setup and assertion.

Mixed included and excluded invoice types:
```java
when(invoicesRepository.getInvoices(customer))
        .thenReturn(new InvoiceInfo(List.of(regularInvoice, excludedInvoice), false, List.of("Acme Downtown")));
```
Expected: the assertion checks that excluded document types are removed while partner warnings stay intact.

Feature-disabled scenario:
```java
when(featureFlagUseCase.execute()).thenReturn(List.of());
verifyNoInteractions(invoiceCollectionEnricher);
```
Expected: the behavior change is asserted through a collaborator interaction, not through internal state.

## Local conventions (the project facts the skill omits)
- Package root: same package as the use case under test, `src/test/java/.../usecase/`.
- Naming shape: `*UseCaseTest`, methods named `given<Setup>_when<Action>_then<Outcome>`.
- Required collaborators / base types: `@ExtendWith(MockitoExtension.class)`, `@Mock` for every
  collaborator port, `@InjectMocks` for the use case under test.
- Config / wiring: none — pure unit test, no Spring context.

## Frequency & coverage (why this earned a skill)
- Occurrences: 90 `*UseCaseTest` files across 14 modules (as of `abc1234`), against 180 `*UseCase`
  production classes.

## Drift / exceptions
- None observed in the sampled exemplar; naming convention is consistent where present.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*UseCaseTest\b' --glob '*.java'`
