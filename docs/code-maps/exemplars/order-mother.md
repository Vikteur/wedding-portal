---
runtime: lazy
generated-by: pattern-scanner
source: order-domain/src/test/java/com/acme/shop/order/domain/OrderMother.java
serves: [object-mother-builders]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `order-domain/.../OrderMother.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
An object mother for the `Order` domain aggregate, shipped as a test-artifact dependency so other
modules' tests can reuse it, not just this module's own tests.

## Named-scenario factory methods + a fluent override builder {#object-mother-builders}
**Serves:** [`object-mother-builders`](../object-mother-builders.md)

Two complementary shapes live in one Mother: (1) named static factories for common shop fixtures
(`standardGadget()`, `standardGadgetNew()`, `expressWidget()`) that read like scenario names rather
than parameter lists; (2) a nested `OrderBuilder` (Lombok `@Setter` + `@Accessors(fluent = true)`)
with sane defaults on every field, for tests that need to override just one or two values. Both
funnel through the same `CreateOrderRequest` construction path the production code uses — a Mother
never bypasses the domain's own construction or validation.

### Source (pseudonymized)
```java
package com.acme.shop.order.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.Month;

public class OrderMother {

    public static Order standardGadget() {
        var creationRequest = CreateOrderRequest.builder()
                .code(OrderCode.of("ORDER-001", OrderCodeType.PARTNER_CATALOG))
                .description("Standard Gadget")
                .placedDate(LocalDate.of(2022, Month.JANUARY, 24))
                .endDate(LocalDate.of(2022, Month.AUGUST, 24))
                .marketCode("ZZ")
                .orderType(OrderType.EXPRESS)
                .remark("expedited shipping")
                .build();
        return new Order(creationRequest);
    }

    public static Order standardGadgetNew() {
        var creationRequest = CreateOrderRequest.builder()
                .code(OrderCode.of("ORDER-001", OrderCodeType.PARTNER_CATALOG))
                .description("Standard Gadget")
                .placedDate(LocalDate.of(2022, Month.JANUARY, 24))
                .endDate(LocalDate.of(2022, Month.AUGUST, 24))
                .marketCode("ZZ")
                .orderType(OrderType.EXPRESS)
                .remark("expedited shipping")
                .salesAgent("Jane Doe")
                .fulfiller("John Doe")
                .fulfilmentCountry("Springfield")
                .notes(java.util.List.of("Priority packing", "Fragile item"))
                .build();
        return new Order(creationRequest);
    }

    public static Order expressWidget() {
        var creationRequest = CreateOrderRequest.builder()
                .code(OrderCode.of("4380960", OrderCodeType.SKU))
                .description("Express Widget")
                .placedDate(LocalDate.of(2022, Month.AUGUST, 24))
                .orderType(OrderType.EXPRESS)
                .remark("priority packaging")
                .build();
        return new Order(creationRequest);
    }

    @Accessors(fluent = true)
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OrderBuilder {
        private OrderCode code = OrderCode.of("2374982734", OrderCodeType.SKU);
        private String description = "Standard Gadget";
        private LocalDate placedDate = LocalDate.of(2022, Month.JANUARY, 24);
        private LocalDate endDate;
        private String marketCode;
        private OrderType orderType = OrderType.OTHER;
        private String remark;
        private String salesAgent;
        private String fulfiller;
        private String fulfilmentCountry;
        private java.util.List<String> notes;

        public static OrderBuilder builder() {
            return new OrderBuilder();
        }

        public Order build() {
            var creationRequest = CreateOrderRequest.builder()
                    .code(code)
                    .description(description)
                    .placedDate(placedDate)
                    .endDate(endDate)
                    .marketCode(marketCode)
                    .orderType(orderType)
                    .remark(remark)
                    .salesAgent(salesAgent)
                    .fulfiller(fulfiller)
                    .fulfilmentCountry(fulfilmentCountry)
                    .notes(notes)
                    .build();
            return new Order(creationRequest);
        }
    }

}
```

### Edge cases
Builder override touches one field only:
```java
var order = OrderMother.OrderBuilder.builder().description("Custom description").build();

assertThat(order.getDescription()).isEqualTo("Custom description");
assertThat(order.getOrderType()).isEqualTo(OrderType.OTHER);
```
Expected: untouched builder defaults remain in place.

Named factory keeps optional fields absent when the scenario does not need them:
```java
var order = OrderMother.expressWidget();

assertThat(order.getEndDate()).isNull();
assertThat(order.getSalesAgent()).isNull();
```
Expected: the factory method stays focused on the scenario, not on populating every nullable field.

Builder can deliberately produce a validation failure:
```java
var order = OrderMother.OrderBuilder.builder().code(null).build();

assertThat(order.validate().errorMessage()).contains("Order.code");
```
Expected: tests can exercise downstream validation paths without inventing ad-hoc fixtures.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*Mother' --glob '*.java'`
