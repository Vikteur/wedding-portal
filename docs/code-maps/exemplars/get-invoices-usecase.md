---
runtime: lazy
source: invoice-usecase/src/main/java/com/acme/shop/invoice/usecase/GetInvoicesUseCase.java
serves: [usecase-orchestration]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `invoice-usecase/.../GetInvoicesUseCase.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A small, typical `*UseCase` — the naming / shape convention used across the codebase instead of a
custom `@UseCase` annotation.

## Plain `*UseCase` class, one `execute()` entry point {#usecase-orchestration}
**Serves:** [`usecase-orchestration`](../usecase-orchestration.md)

`public class GetInvoicesUseCase` has no interface and no annotation beyond
`@RequiredArgsConstructor` + `@Transactional`; a single public `execute()` method resolves the
current principal via `AuthenticationFacade.getAuthentication()` and delegates to a repository port.
Filtering rules (`EXCLUDED_DOCUMENT_TYPES`) live in the use case, not the repository or controller.

### Source (pseudonymized)
```java
package com.acme.shop.invoice.usecase;

import com.acme.shop.authentication.repository.AuthenticationFacade;
import com.acme.shop.storefront.domain.featureflag.Feature;
import com.acme.shop.storefront.usecase.GetFeatureFlagUseCase;
import com.acme.shop.invoice.domain.InvoiceDocumentType;
import com.acme.shop.invoice.domain.InvoiceInfo;
import com.acme.shop.invoice.repository.InvoicesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Transactional
@RequiredArgsConstructor
public class GetInvoicesUseCase {
    private final InvoicesRepository invoicesRepository;
    private final AuthenticationFacade authenticationFacade;
    private final InvoiceCollectionEnricher invoiceCollectionEnricher;
    private final GetFeatureFlagUseCase featureFlagUseCase;

    private static final Set<InvoiceDocumentType> EXCLUDED_DOCUMENT_TYPES = Set.of(
            InvoiceDocumentType.CAMPAIGN_SUMMARY,
            InvoiceDocumentType.SUPPLY_PLAN,
            InvoiceDocumentType.ACTIVITY_NOTE,
            InvoiceDocumentType.CATALOG_SNAPSHOT,
            InvoiceDocumentType.ORDER_CONFIRMATION,
            InvoiceDocumentType.WAREHOUSE_PLAN
    );

    public InvoiceInfo execute() {
        var invoiceInfo = invoicesRepository.getInvoices(authenticationFacade.getAuthentication());

        var invoiceSummaries = invoiceInfo.invoiceSummaries().stream()
                .filter(summary -> !EXCLUDED_DOCUMENT_TYPES.contains(summary.documentType()))
                .toList();

        return new InvoiceInfo(
                collectionsEnabled() ? invoiceCollectionEnricher.enrich(invoiceSummaries) : invoiceSummaries,
                invoiceInfo.customerArchived(),
                invoiceInfo.unavailableResultsFromPartners()
        );
    }

    private boolean collectionsEnabled() {
        return featureFlagUseCase.execute().contains(Feature.ENABLE_ADD_INVOICE_TO_COLLECTION);
    }
}
```

### Edge cases
Repository returns only excluded document types:
```java
var invoiceSummaries = invoiceInfo.invoiceSummaries().stream()
        .filter(summary -> !EXCLUDED_DOCUMENT_TYPES.contains(summary.documentType()))
        .toList();
```
Expected: the returned `InvoiceInfo` contains an empty summary list but preserves the rest of the metadata.

Collection feature flag is disabled:
```java
collectionsEnabled() ? invoiceCollectionEnricher.enrich(invoiceSummaries) : invoiceSummaries
```
Expected: enrichment is skipped entirely and the filtered summaries are returned as-is.

Partner systems report unavailable results:
```java
return new InvoiceInfo(invoiceSummaries, invoiceInfo.customerArchived(), invoiceInfo.unavailableResultsFromPartners());
```
Expected: the use case keeps the partner gap list intact while still applying filtering and optional enrichment.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*UseCase\b' --glob '*.java'`
