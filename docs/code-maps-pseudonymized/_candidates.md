---
runtime: design
---
<!-- AI_DISCLAIMER v1.0 -->
# Candidate skills — pattern-scanner harvest (`shop-backend`, backend)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> Patterns present in the codebase but either below the frequency threshold (≥3 files across ≥2
> modules) or matching no existing skill. Below threshold ⇒ a candidate, not a code-map. A skill
> with no observed occurrences is flagged dead in the section below instead. Scanned at: `abc1234`;
> the `soap-cxf-gateway`, `archunit-fitness`, and `domain-events` rows were re-verified and corrected
> at `abc1234` (see notes).

## Below threshold (matches an existing skill, not yet a code-map)

| Skill | Signal probed | Files | Modules | Note |
|-------|---------------|-------|---------|------|
| `testcontainers-postgres` | `PostgreSQLContainer` | 2 | 2 (`application`, `authentication-adapter`) | One file short of the file threshold; re-scan as more modules adopt Postgres-backed integration tests. |
| `testcontainers-services` | `GenericContainer` / `@Testcontainers` | 2 | 1 (`storefront-adapter`, Typesense + Smtp4dev initializers) | Below both thresholds. |
| `notification-pattern` (async notifications/channel adapters) | `class *Notification(er)?` | 2 | 1 | No async-delivery notification pattern found (re-verified at `abc1234`: 0 real hits); the frequent `Notification` class is the unrelated validation pattern (see `validation-notification-result` below). **De-scoped from the backend side** (hub manifest, `patterns.notification` block keeps it on-demand) — no map; re-scope + harvest when a first implementation lands. |

Resolved former rows — details now live in their maps, not repeated here: `soap-cxf-gateway` and
`archunit-fitness` were below threshold but are recorded as thin maps
([soap-cxf-gateway], [archunit-fitness]).

## Scan-correction log (first-harvest misses, since resolved)

- `domain-events` — first harvest probed Spring's `ApplicationEventPublisher` directly and read
  below-threshold; the project wraps it behind its own `DomainEvent`/`DomainEventPublisher` port
  with `@TransactionalEventListener` listeners. Corrected map: [domain-events]
  (probe details there).
- `identifier-pseudonymization` — clearly above threshold but the first harvest recorded no row at
  all. Map: [identifier-pseudonymization].

## Frequent, no matching skill (candidate for `skill-author`)

| Proposed skill | Signal | Files | Modules | Exemplar | Notes |
|-----------------|--------|-------|---------|----------|-------|
| `validation-notification-result` | `com.acme.shop.common.domain.validation.Notification` (a Result/Either-style object carrying errors, `hasErrors()`/`errorMessage()`) | 179 | 32 | [`Notification.java`](exemplars/notification.md#validation-notification-result) — `common-domain/.../common/domain/validation/Notification.java`, full source embedded in the leaf | **Authored** — the generic skill now exists in the hub (`skills/validation-notification-result/SKILL.md`); this repo's map is [validation-notification-result] (indexes the `Notification.java` and `CatalogSearchSchedulerConfig.java` exemplars). It is the GoF "Notification" validation pattern (a collected-errors result object), deliberately named to not collide with the async-delivery `notification-pattern` skill. |

Core of the signal, for orientation (full class in the exemplar leaf):

```java
@EqualsAndHashCode
@Getter
public class Notification {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    public String errorMessage() {
        return String.join(", ", errors);
    }
}
```

## Dead (no occurrences — do not wire for this project)
- None of the scanned backend skills scored zero; every skill above the meta/process tier had at
  least a below-threshold hit. Meta/process skills (`clean-code`, `clean-architecture`,
  `architecture-blueprint`, `java`, `jvm-testing`, `junit-tests`, `tdd-loop`, `agent-orchestration`,
  `cross-repo-orchestration`, `contract-slice`, `openapi-authoring`, `contract-apply-pr`,
  `failure-triage`, `design-review`, `release-changelog`, `conventional-commits`,
  `repo-structure-analysis`, `architecture-analysis`, `stack-detection`,
  `test-architecture-analysis`, `build-ci-analysis`, `capability-mapping`,
  `integration-analysis`, `code-review-process`, `security-review`, `openapi-contract-workflow`)
  describe process/analysis rather than a single grep-able code shape, so they were not probed by
  this scan and are out of scope for a code-map.

### Edge cases
Signal probes that look like hits but are not — each is a false positive the scan had to reject:

An unrelated class that merely ends in `Notification` (async-delivery shape, not the validation pattern):
```java
public class OrderShippedNotification {
    private final String recipient;
    private final String channel;   // "EMAIL" | "SMS" — delivery concern, no errors/warnings lists
}
```
Expected: not counted for `validation-notification-result` (no `hasErrors()`/`errorMessage()`); at most a `notification-pattern` row.

A Spring `ApplicationEventPublisher` call that is a raw probe hit for `domain-events` but really goes through the project port:
```java
private final DomainEventPublisher domainEventPublisher;   // project port, wraps ApplicationEventPublisher
```
Expected: counted under the corrected [domain-events] map, not as a raw-publisher violation.

A Testcontainers import that sits in a build file only (no test class uses it):
```kotlin
testImplementation("org.testcontainers:postgresql")
```
Expected: not counted as a file hit for `testcontainers-postgres`; only `*.java` files with `PostgreSQLContainer` count.

[soap-cxf-gateway]: soap-cxf-gateway.md
[archunit-fitness]: archunit-fitness.md
[domain-events]: domain-events.md
[identifier-pseudonymization]: identifier-pseudonymization.md
[validation-notification-result]: validation-notification-result.md
