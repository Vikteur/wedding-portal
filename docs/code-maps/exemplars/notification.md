---
runtime: lazy
source: common-domain/src/main/java/com/acme/shop/common/domain/validation/Notification.java
serves: [validation-notification-result]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `common-domain/.../validation/Notification.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The repo-wide collected-errors result: two message lists (errors + warnings) with
`hasErrors()` / `hasWarnings()`, comma-joined `errorMessage()` / `warningMessage()` accessors, and
composition helpers.

## Collected-errors result object with composition helpers {#validation-notification-result}
**Serves:** [`validation-notification-result`](../validation-notification-result.md)

A deliberately small collector: `addError` / `addWarning` mutate, Lombok `@Getter @EqualsAndHashCode`
provides value semantics, and composition is first-class — static `empty()` and `of(error)`
factories, `merge(Notification...)` for combining sibling failures, and a fluent
`addNotification(child)` for folding nested results into the current one. Field-level rules append
into a passed-in notification so one validation pass can report every failure at once.

### Source (pseudonymized)
```java
package com.acme.shop.common.domain.validation;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode
@Getter
public class Notification {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    public void addWarning(final String warning) {
        this.warnings.add(warning);
    }

    public boolean hasWarnings() {
        return !this.warnings.isEmpty();
    }

    public String errorMessage() {
        return String.join(", ", errors);
    }

    public String warningMessage() {
        return String.join(", ", warnings);
    }

    public static Notification empty() {
        return new Notification();
    }

    public static Notification of(String error) {
        var notification = empty();
        notification.addError(error);
        return notification;
    }

    public static Notification merge(Notification... notifications) {
        var newNotification = empty();
        Arrays.stream(notifications).forEach(notification -> newNotification.getErrors().addAll(notification.getErrors()));
        return newNotification;
    }

    public Notification addNotification(Notification notification) {
        if (notification == null) {
            return this;
        }

        errors.addAll(notification.getErrors());
        warnings.addAll(notification.getWarnings());

        return this;
    }
}
```

### Edge cases
Creating a notification with one error:
```java
var notification = Notification.of("Order.code is required");
```
Expected: the helper returns a ready-to-use collector with a single error entry.

Merging notifications that only differ by warnings:
```java
var left = Notification.empty();
left.addWarning("Optional note trimmed");
var right = Notification.of("Order.remark is required");
var merged = Notification.merge(left, right);
```
Expected: `merged` keeps the error from `right`; warning-only state does not flow through `merge(...)`.

Adding a nested notification preserves warnings too:
```java
var parent = Notification.empty();
var child = Notification.empty();
child.addWarning("Order.slug normalized");
parent.addNotification(child);
```
Expected: `addNotification(...)` copies warnings and errors from the child into the parent.

## Provenance
- Scanned at: `abc1234` · tool/query: `grep -rl 'common.domain.validation.Notification' --include='*.java'`
