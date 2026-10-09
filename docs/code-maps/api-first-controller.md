---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `api-first-controller` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`CustomerController.java`](exemplars/customer-controller.md#api-first-controller) | `implements <Capability>Api`, delegates to use-cases, maps via a static `*ToDTOMapper` | adapter (`account-adapter`) |

### Excerpts
Generated-contract controller (full source in the [exemplar leaf](exemplars/customer-controller.md#api-first-controller)):
```java
public class CustomerController implements AccountApi {
    @Override
    public ResponseEntity<List<DelegationDTO>> getAllDelegations() {
        var delegations = getDelegationsUseCase.execute().stream()
                .map(DelegationToDTOMapper::map).toList();
```

Controller-owned route beside generated overrides:
```java
@GetMapping("api/delegation/verify-topic-access/{topic}")
public ResponseEntity<Boolean> verifyDelegationForTopic(@PathVariable Topic topic) {
    return ResponseEntity.ok()
            .body(verifyDelegationForTopicAccessUseCase.execute(topic));
}
```

### Edge cases
Generated override plus one manual route in the same controller:
```java
assertThat(CustomerController.class.getInterfaces()).contains(AccountApi.class);
assertThat(CustomerController.class.getDeclaredMethod("verifyDelegationForTopic", Topic.class)).isNotNull();
```
Expected: contract methods stay on the generated interface; controller-specific routes can coexist when the contract has not absorbed them yet.

Boundary mapping after a successful read:
```java
var dto = DelegationToDTOMapper.map(delegation);
assertThat(dto.customerName()).isEqualTo("Jane Doe");
```
Expected: the controller maps domain output through a dedicated mapper instead of constructing DTOs inline.

Use-case validation failure on a write endpoint:
```java
var response = controller.recordLastVisit();
assertThat(response.getStatusCode().value()).isEqualTo(400);
assertThat(response.getBody().errors()).contains("Could not record Springfield storefront visit");
```
Expected: write endpoints translate the returned notification into an HTTP response without leaking domain internals.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. The
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [adapter/controller](scaffold/adapter/controller.mustache) | `<capability>-adapter/src/main/java/<pkg>/adapter/controller/<Name>Controller.java` | `package`, `Name`, `Api`, `UseCase`, `useCase`, `operationId`, `Domain`, optional `topic` (all from the contract's tag and operation) | path/query parameters and request bodies; non-200 responses; more operations on the same `Api`; list results |

The mapper it calls comes from `domain-dto-mapper`; its test from `jvm-testing` (controller-test).

## Local conventions (the project facts the skill omits)
- Package root: `<capability>-adapter/src/main/java/com/acme/shop/<capability>/adapter/controller/`.
- Naming shape: `*Controller`, `implements <Capability>Api` (the generated interface from
  `shop.openapi-conventions.gradle.kts`).
- Required collaborators: one `*UseCase` per endpoint, constructor-injected; a static
  `*ToDTOMapper` for the domain→DTO step (never inline DTO construction).
- Config / wiring: `@RestController`, no extra Spring config beyond the generated Api interface.

## Frequency & coverage (why this earned a skill)
- Occurrences: 33 files across 14 modules (as of `abc1234`).
- Hotspots: every capability adapter with a REST surface (`access-matrix-adapter`,
  `product-restriction-adapter`, `application`, `authentication-adapter`, `promotion-adapter`,
  `payments-gateway`, `storefront-adapter`, `partner-gateway`, `merchant-adapter`,
  `campaign-adapter`, `account-adapter`, `attribute-mapping-adapter`, `invoice-adapter`,
  `order-adapter`).

## Drift / exceptions
- Not every controller method is a generated-interface override — a controller may add a
  plain-`@GetMapping` route alongside the generated ones (see `CustomerController.verifyDelegationForTopic`)
  when the OpenAPI contract does not yet cover that endpoint. Treat as intentional, not drift.

## Provenance
- Scanned at: `abc1234` · tool/query: `scripts/scan-patterns.sh --glob '*.java' '@RestController'`
