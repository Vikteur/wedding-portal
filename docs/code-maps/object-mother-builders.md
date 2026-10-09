---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `object-mother-builders` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`OrderMother.java`](exemplars/order-mother.md#object-mother-builders) | Named-scenario static factories + a fluent override builder, shipped as a test-jar | domain test (`order-domain`) |

### Excerpts
Named-scenario factory (full source in the [exemplar leaf](exemplars/order-mother.md#object-mother-builders)):
```java
public static Order standardGadget() {
    var creationRequest = CreateOrderRequest.builder()
            .code(OrderCode.of("4380960", OrderCodeType.SKU))
            .description("Standard Gadget")
            .placedDate(LocalDate.of(2022, Month.AUGUST, 24))
            .orderType(OrderType.EXPRESS)
            .remark("priority packaging")
            .build();
    return new Order(creationRequest);
}
```

Consuming the Mother from another module's tests (`build.gradle.kts`):
```kotlin
testImplementation(project(":order-domain", "testArtifacts"))
```

### Edge cases
Override a single field and keep every other default:
```java
var order = OrderMother.OrderBuilder.builder().description("Custom description").build();

assertThat(order.getDescription()).isEqualTo("Custom description");
assertThat(order.getOrderType()).isEqualTo(OrderType.OTHER);
```
Expected: untouched fields keep the builder defaults.

Deliberately invalid fixture, to exercise validation paths:
```java
var order = OrderMother.OrderBuilder.builder().code(null).build();

assertThat(order.validate().errorMessage()).isEqualTo("Field 'Order.code' should not be null");
```
Expected: the Mother builds the object along the same construction path as production; `validate()` reports the problem.

Optional fields left `null` by a scenario factory:
```java
var order = OrderMother.standardGadget();

assertThat(order.getEndDate()).isNull();
assertThat(order.getNotes()).isNull();
```
Expected: scenario factories only populate what the scenario needs.

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. The
template's header comment lists its exact variables; the script refuses to overwrite an existing file.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [domain/mother](scaffold/domain/mother.mustache) | `<capability>-domain/src/test/java/<pkg>/domain/<Name>Mother.java` | `package`, `Name`, `fixture`, `fields[type, name, example]` (`example` is a Java literal that satisfies the invariants) | more named scenarios; a record type (`build()` uses the record's builder, not a request); imports for field types |

Example:
```bash
scripts/scaffold.sh domain/mother '{"package":"<pkg>","Name":"Order","fixture":"standardGadget","fields":[{"type":"String","name":"description","example":"\"Standard Gadget\""}]}' order-domain/src/test/java/<pkg path>/domain/OrderMother.java
```

## Local conventions (the project facts the skill omits)
- Package root: `<capability>-domain/src/test/java/.../<capability>/domain/`, exported to other
  modules' tests via the shared test-artifacts convention plugin.
- Naming shape: `<Aggregate>Mother`, with named static factory methods for common fixtures
  (`expressWidget()`, `standardGadget()`) plus an optional nested `<Aggregate>Builder` for ad-hoc overrides.
- Required collaborators / base types: routes through the real domain construction path (the
  aggregate's own factory/constructor) — never bypasses domain invariants.
- Config / wiring: none.

## Frequency & coverage (why this earned a skill)
- Occurrences: 62 `*Mother` classes across 18 modules (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*Mother' --glob '*.java'`
