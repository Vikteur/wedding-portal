---
runtime: lazy
generated-by: pattern-scanner
source: invoice-usecase/src/test/java/com/acme/shop/invoice/usecase/GetInvoicesUseCaseTest.java
serves: [usecase-bdd-spec, junit-tests, jvm-testing]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `invoice-usecase/.../GetInvoicesUseCaseTest.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The unit test for `GetInvoicesUseCase` (see [get-invoices-usecase](get-invoices-usecase.md#usecase-orchestration)).

## `given..._when..._then...` test naming {#usecase-bdd-spec}
**Serves:** [`usecase-bdd-spec`](../usecase-bdd-spec.md)

`@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`, no test framework beyond JUnit 5,
Mockito, and AssertJ. Method names spell out the full scenario in one BDD-shaped sentence rather
than relying on `@DisplayName` or Given/When/Then comment blocks. `@ParameterizedTest` +
`@EnumSource` covers the exhaustive excluded-document-type cases.

## JUnit 5 test anatomy {#junit-tests}
**Serves:** [`junit-tests`](../junit-tests.md)

The canonical shape of a backend unit test: JUnit 5 + Mockito + AssertJ only, no further test
framework. The Given/When/Then structure is carried by the method name, `assertThat` is the
assertion idiom, and `@ParameterizedTest` replaces copy-pasted per-enum tests.

## Behavior-spec testing at the unit level {#jvm-testing}
**Serves:** [`jvm-testing`](../jvm-testing.md)

Mocking stays at the ports: repositories / gateways are mocked, the use case under test gets
`@InjectMocks`, and collaborator values are real objects supplied by Mothers. One behavior per test
method; outcomes are asserted on the returned value or collaborator interaction, not internals.

### Source (pseudonymized)
```java
package com.acme.shop.invoice.usecase;

import com.acme.shop.authentication.domain.AuthenticatedCustomer;
import com.acme.shop.authentication.repository.AuthenticationFacade;
import com.acme.shop.storefront.domain.featureflag.Feature;
import com.acme.shop.storefront.usecase.GetFeatureFlagUseCase;
import com.acme.shop.invoice.domain.InvoiceCollectionMother;
import com.acme.shop.invoice.domain.InvoiceDocumentType;
import com.acme.shop.invoice.domain.InvoiceId;
import com.acme.shop.invoice.domain.InvoiceInfo;
import com.acme.shop.invoice.domain.InvoiceSummary;
import com.acme.shop.invoice.domain.InvoiceSummaryMother;
import com.acme.shop.invoice.repository.InvoicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInvoicesUseCaseTest {
    @Mock
    private InvoicesRepository invoicesRepository;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private InvoiceCollectionEnricher invoiceCollectionEnricher;

    @Mock
    private GetFeatureFlagUseCase featureFlagUseCase;

    @Mock
    private AuthenticatedCustomer customer;

    @InjectMocks
    private GetInvoicesUseCase getInvoicesUseCase;

    @BeforeEach
    void setUp() {
        when(authenticationFacade.getAuthentication()).thenReturn(customer);
        when(featureFlagUseCase.execute()).thenReturn(List.of(Feature.ENABLE_ADD_INVOICE_TO_COLLECTION));
        lenient().when(invoiceCollectionEnricher.enrich(anyList())).thenAnswer(returnsFirstArg());
    }

    @Test
    void givenAuthenticatedCustomerAndNoInvoicesAndAccountIsActiveAndNoResultsAreUnavailableFromPartners_whenGetInvoices_thenReturnEmptyList() {
        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(), false, List.of()));

        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));

        verify(invoicesRepository).getInvoices(customer);
    }

    @Test
    void givenAuthenticatedCustomerAndNoInvoicesAndAccountIsArchivedAndNoResultsAreUnavailableFromPartners_whenGetInvoices_thenReturnEmptyList() {
        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(), true, List.of()));

        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), true, List.of()));

        verify(invoicesRepository).getInvoices(customer);
    }

    @Test
    void givenAuthenticatedCustomerAndAvailableInvoicesAndAccountIsActiveAndResultsAreUnavailableFromPartners_whenGetInvoices_thenReturnListOfInvoices() {
        var invoice = InvoiceSummaryMother.sampleOrderInvoice();

        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(invoice), false, List.of("Acme Springfield")));
        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(invoice), false, List.of("Acme Springfield")));

        verify(invoicesRepository).getInvoices(customer);
    }

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
        var excludedInvoice = excludedInvoice(documentType);

        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(excludedInvoice), false, List.of()));
        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));

        verify(invoicesRepository).getInvoices(customer);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceDocumentType.class, names = {
            "CAMPAIGN_SUMMARY",
            "SUPPLY_PLAN",
            "ACTIVITY_NOTE",
            "CATALOG_SNAPSHOT",
            "ORDER_CONFIRMATION",
            "WAREHOUSE_PLAN"
    })
    void givenMixedInvoices_whenGetInvoices_thenReturnListOfInvoicesWithoutExcludedDocumentTypes(InvoiceDocumentType documentType) {
        var regularInvoice = InvoiceSummaryMother.sampleOrderInvoice();
        var excludedInvoice = excludedInvoice(documentType);

        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(regularInvoice, excludedInvoice), false, List.of("Acme Downtown")));

        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(regularInvoice), false, List.of("Acme Downtown")));

        verify(invoicesRepository).getInvoices(customer);
    }

    @Test
    void givenInvoices_whenGetInvoices_thenOnlyTheNonExcludedSummariesAreEnriched() {
        var regularInvoice = InvoiceSummaryMother.sampleOrderInvoice();
        var excludedInvoice = excludedInvoice(InvoiceDocumentType.CATALOG_SNAPSHOT);
        when(invoicesRepository.getInvoices(customer))
                .thenReturn(new InvoiceInfo(List.of(regularInvoice, excludedInvoice), false, List.of()));

        getInvoicesUseCase.execute();

        verify(invoiceCollectionEnricher).enrich(List.of(regularInvoice));
    }

    @Test
    void givenFeatureDisabled_whenGetInvoices_thenSummariesAreNotEnriched() {
        var invoice = InvoiceSummaryMother.sampleOrderInvoice();
        when(featureFlagUseCase.execute()).thenReturn(List.of());
        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(invoice), false, List.of()));

        assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(invoice), false, List.of()));

        verifyNoInteractions(invoiceCollectionEnricher);
    }

    @Test
    void givenEnrichedSummaries_whenGetInvoices_thenTheEnrichedSummariesAreReturned() {
        var invoice = InvoiceSummaryMother.sampleOrderInvoice();
        var enriched = invoice.withCollectionIds(List.of(InvoiceCollectionMother.FAVOURITES_ID, InvoiceCollectionMother.IMPORTANT_ID));
        when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(invoice), false, List.of()));
        when(invoiceCollectionEnricher.enrich(List.of(invoice))).thenReturn(List.of(enriched));

        assertThat(getInvoicesUseCase.execute())
                .isEqualTo(new InvoiceInfo(List.of(enriched), false, List.of()));
    }

    private static InvoiceSummary excludedInvoice(final InvoiceDocumentType documentType) {
        return InvoiceSummary.builder()
                .id(InvoiceId.builder()
                        .id("ACME-INV-2419106")
                        .type("LOCAL")
                        .externalId("LOCAL")
                        .version("1.0")
                        .build())
                .documentType(documentType)
                .date(LocalDateTime.of(2024, 2, 2, 13, 59, 59))
                .vendorName("Jane Doe")
                .build();
    }
}
```

### Edge cases
No invoices and no unavailable partner results:
```java
when(invoicesRepository.getInvoices(customer)).thenReturn(new InvoiceInfo(List.of(), false, List.of()));
assertThat(getInvoicesUseCase.execute()).isEqualTo(new InvoiceInfo(List.of(), false, List.of()));
```
Expected: the test name carries the scenario; the assertion checks the full returned value object.

Mixed invoice types:
```java
when(invoicesRepository.getInvoices(customer))
        .thenReturn(new InvoiceInfo(List.of(regularInvoice, excludedInvoice), false, List.of("Acme Downtown")));
```
Expected: only the regular invoice survives filtering while the partner-gap list stays unchanged.

Feature disabled branch:
```java
when(featureFlagUseCase.execute()).thenReturn(List.of());
verifyNoInteractions(invoiceCollectionEnricher);
```
Expected: the use case returns un-enriched summaries and never touches the enricher.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*UseCaseTest\b' --glob '*.java'`
