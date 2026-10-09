---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `usecase-orchestration` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`GetInvoicesUseCase.java`](exemplars/get-invoices-usecase.md#usecase-orchestration) | Plain `*UseCase` class, one `execute()` entry point, resolves the principal via `AuthenticationFacade` | usecase (`invoice-usecase`) |

### Excerpts
Single public orchestration entry point:
```java
public InvoiceInfo execute() {
    var invoiceInfo = invoicesRepository.getInvoices(authenticationFacade.getAuthentication());

    var invoiceSummaries = invoiceInfo.invoiceSummaries().stream()
            .filter(summary -> !EXCLUDED_DOCUMENT_TYPES.contains(summary.documentType()))
            .toList();
```

Feature-flagged enrichment stays inside the use case:
```java
return new InvoiceInfo(
        collectionsEnabled() ? invoiceCollectionEnricher.enrich(invoiceSummaries) : invoiceSummaries,
        invoiceInfo.customerArchived(),
        invoiceInfo.unavailableResultsFromPartners()
);
```

### Edge cases
Only excluded document types are returned from the repository:
```java
when(invoicesRepository.getInvoices(customer))
        .thenReturn(new InvoiceInfo(List.of(excludedInvoice), false, List.of()));
```
Expected: `execute()` returns an empty summary list instead of leaking filtered types past the use case.

Feature flag is disabled:
```java
when(featureFlagUseCase.execute()).thenReturn(List.of());
var result = getInvoicesUseCase.execute();
```
Expected: summaries are returned unchanged and `invoiceCollectionEnricher` is not called.

Partners report partial availability:
```java
new InvoiceInfo(List.of(invoice), false, List.of("Acme Springfield"));
```
Expected: `unavailableResultsFromPartners()` is preserved as-is while the use case still filters summaries.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. Each
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [usecase/repository-port](scaffold/usecase/repository-port.mustache) | `<capability>-usecase/src/main/java/<pkg>/repository/<Name>Repository.java` | `package`, `basePackage`, `Name`, `Result`, `method`, `authenticated` | more methods; parameters beyond the authenticated customer |
| [usecase/usecase](scaffold/usecase/usecase.mustache) | `.../usecase/<Name>UseCase.java` | `package`, `Name`, `Result`, `Repository`, `repository`, `method`, `authenticated`, `basePackage` (when authenticated) | a request input; creation through a factory and its Notification; several ports; events; writes |

The base case is a read: `execute()` returns one port call. Its test comes from `jvm-testing` (usecase-test).

## Local conventions (the project facts the skill omits)
- Package root: `<capability>-usecase/src/main/java/com/acme/shop/<capability>/usecase/`.
- Naming shape: `*UseCase` — there is no `@UseCase` annotation in this codebase; the class-name
  suffix alone is the marker.
- Required collaborators / base types: no interface, no base class. `@RequiredArgsConstructor` +
  optional `@Transactional`. Entry point is always `execute(...)` (no-arg or with a request object).
- Config / wiring: plain Spring bean (constructor injection), no special annotation.

## Frequency & coverage (why this earned a skill)
- Occurrences: 180 files across 14 `*-usecase` modules (as of `abc1234`).
- Hotspots: every capability's `*-usecase` module.

## Drift / exceptions
- None observed — the `*UseCase` + `execute()` shape is uniform across all 14 modules.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*UseCase\b' --glob '*.java'`
