---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `domain-modeling` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`OrderCode.java`](exemplars/order-code.md#domain-modeling) | Package-private constructor + static factory, `Objects.requireNonNull` invariants, Lombok value semantics | domain (`order-domain`) |

### Excerpts
Hand-written value object (full source in the [exemplar leaf](exemplars/order-code.md#domain-modeling)):
```java
OrderCode(String code, OrderCodeType type) {
    this.code = Objects.requireNonNull(code);
    this.type = Objects.requireNonNull(type);
}

public static OrderCode of(final String value, final OrderCodeType type) {
    return new OrderCode(value, type);
}
```

Plain `record` shape, used when no custom validation is needed (`account-domain`):
```java
@Builder
public record CustomerAddress(
         SalesRegion region,
         String regionCode,
         String cityName,
         String postalCode
) implements Serializable { }
```

### Edge cases
Record with a `null` component (records do not null-check by themselves):
```java
var address = CustomerAddress.builder().cityName("Springfield").build();
assertThat(address.region()).isNull();
assertThat(address.postalCode()).isNull();
```
Expected: construction succeeds; null checks, if needed, belong in a validating factory (see [validation-notification-result]).

Value equality on records:
```java
var a = CustomerAddress.builder().cityName("Springfield").postalCode("00000").build();
var b = CustomerAddress.builder().cityName("Springfield").postalCode("00000").build();
assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
```
Expected: equal by component values; no `@EqualsAndHashCode` needed on a record.

Hand-written value object with an invalid argument:
```java
assertThatThrownBy(() -> OrderCode.of(null, OrderCodeType.SKU))
        .isInstanceOf(NullPointerException.class);
```
Expected: fails fast at construction — no half-built instance exists.

## Local conventions (the project facts the skill omits)
- Package root: `<capability>-domain/src/main/java/.../domain/`.
- Naming shape: two coexisting shapes — a plain `record` for value objects with no custom
  validation (for example `CustomerAddress` in `account-domain`), and a Lombok class with a
  package-private constructor + static `of(...)`/factory method when construction needs to validate
  or Lombok's generated equality is preferred (for example `OrderCode`).
- Required collaborators / base types: `Objects.requireNonNull` for fail-fast invariant checks;
  `@Getter @EqualsAndHashCode @ToString` when not using a `record`.
- Config / wiring: none.

## Frequency & coverage (why this earned a skill)
- Occurrences: 105 `record` declarations across 21 modules (as of `abc1234`) — the more common
  shape; the hand-written value-object shape (like `OrderCode`) is used specifically when a factory
  needs to run validation logic.

## Drift / exceptions
- Two value-object shapes coexist by design (record vs. Lombok class + factory) — not drift, pick
  based on whether construction needs validation beyond what a canonical record constructor
  expresses tersely.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '^\s*(public\s+)?record\s+[A-Za-z]+' --glob '*.java'`

[validation-notification-result]: validation-notification-result.md
