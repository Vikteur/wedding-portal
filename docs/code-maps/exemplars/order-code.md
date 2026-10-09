---
runtime: lazy
generated-by: pattern-scanner
source: order-domain/src/main/java/com/acme/shop/order/domain/OrderCode.java
serves: [domain-modeling]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `order-domain/.../OrderCode.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A small immutable value object identifying an order by code + coding system.

## Package-private constructor + static factory {#domain-modeling}
**Serves:** [`domain-modeling`](../domain-modeling.md)

Constructor is package-private (no `public`), validated with `Objects.requireNonNull` on both
fields (fails fast at construction, never a half-built instance) and exposed only through the
static factory `OrderCode.of(value, type)`. `@Getter @EqualsAndHashCode @ToString` (Lombok)
provides value semantics without hand-written boilerplate. Where the value object does not need
custom validation or behavior beyond field access, the codebase reaches for a plain `record`
instead — this hand-written shape is the local convention specifically when the factory needs to run
validation a canonical record constructor cannot express as tersely, or when Lombok-generated
equality is preferred.

### Source (pseudonymized)
```java
package com.acme.shop.order.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@EqualsAndHashCode
@ToString
public class OrderCode {

    private final String code;
    private final OrderCodeType type;

    OrderCode(String code, OrderCodeType type) {
        this.code = Objects.requireNonNull(code);
        this.type = Objects.requireNonNull(type);
    }

    public static OrderCode of(final String value,
                               final OrderCodeType type) {
        return new OrderCode(value, type);
    }

    public String getTypeAndCode() {
        return type.name() + "." + code;
    }
}
```

### Edge cases
Null code rejected immediately:
```java
assertThatThrownBy(() -> OrderCode.of(null, OrderCodeType.SKU))
        .isInstanceOf(NullPointerException.class);
```
Expected: construction fails fast before an invalid instance exists.

Null code type rejected immediately:
```java
assertThatThrownBy(() -> OrderCode.of("4380960", null))
        .isInstanceOf(NullPointerException.class);
```
Expected: the constructor enforces both invariants, not only the value field.

Type/code rendering is deterministic:
```java
var code = OrderCode.of("4380960", OrderCodeType.SKU);
assertThat(code.getTypeAndCode()).isEqualTo("SKU.4380960");
```
Expected: callers get a stable cache/log token without re-assembling the string themselves.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '^\s*(public\s+)?record\s+[A-Za-z]+' --glob '*.java'`
