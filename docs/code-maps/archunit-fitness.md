# Code map: ArchUnit fitness

Decided in architecture-conventions §3 (line 199); built by TASK-3.1. The suite lives in `application/src/test/java/app/rekord/architecture` and checks rules A1 to A14. ArchUnit is `archunit-junit5` 1.5.1, pinned as `archunit` in `gradle/libs.versions.toml`.

**The suite.** Five package-private classes, each `@AnalyzeClasses(locations = ProductionClasses.class)` with one `@ArchTest static final ArchRule` per rule. The rules themselves are in the matching `*Rules` classes, and every one carries a `because(...)` that starts with its id.

| Class | Rules | Source |
|---|---|---|
| `DomainArchitectureTest` | A1 | FW-C-01 |
| `LayerArchitectureTest` | A2, A10 | FW-C-02 |
| `UseCaseArchitectureTest` | A3 | FW-C-15, FW-C-18, 18 C-06, CT-11 |
| `AdapterArchitectureTest` | A4, A5, A6, A11, A14 | FW-C-27, FW-C-28, FW-C-37, FW-C-47; A14 is architecture-conventions §7.1 and TASK-3.1 |
| `ProductionCodeArchitectureTest` | A7, A8, A9, A12, A13 | FW-C-20, FW-C-32, FW-C-59, FW-C-57, FW-C-58, FW-C-10, FW-C-30 |

**The import.** `ProductionClasses` is a `LocationProvider` that reads the release-25 class files in `build/classes/java/main` of all six modules (domain, usecase, adapter, gateway, application, logging), so the rules see the whole product, not one module. Rule behaviour is tested separately against fixtures that `FixtureCompiler` compiles at test time (`--release 25`), so no fixture class sits under a layer package.

**The store.** Every rule is wrapped in `FreezingArchRule.freeze(...)`. The store is `application/src/test/archunit_store/`: `stored.rules` plus one file per rule, and every file is empty because nothing is frozen. `application/src/test/resources/archunit.properties` sets `freeze.store.default.allowStoreCreation=false` and `freeze.store.default.allowStoreUpdate=false`, so a new violation always fails and the store never rewrites itself. A rule whose `because` text is renamed, or a new rule, needs its empty entry added to the store deliberately (set both flags to `true`, run the suite once, check every file is still empty, set them back). Freezing a real violation needs a tracked reason. `FrozenBaselineTest` and `ArchitectureSuiteTest` guard all of this.

**Package roots per layer** (A §2.3, line 156):


**Exceptions written into the rules** (never into the baseline):

- **A3 (CT-11, A line 1096; 18 C-06, line 533).** `app.rekord.usecase..` depends only on `java..`, `app.rekord.domain..`, `app.rekord.usecase..` and the allow-list of §6.2. The one exception allows `jakarta.transaction.Transactional` (the transaction at the use case, FW-C-14), its member type `jakarta.transaction.Transactional$TxType` (ArchUnit reports it as a separate dependency), and `jakarta.enterprise.context.ApplicationScoped` (ArC needs a bean for the interceptor). A use case is never the result of an `@Produces` method or field: A3 also refuses a producer method that returns or constructs a type of `app.rekord.usecase..` and a producer field of such a type (ports in `app.rekord.usecase..port..` stay producible), because ArC binds `@Transactional` to managed beans by default and not to the result of a producer (UD-15.a, PIN-AC-0448; observed by `UseCaseTransactionBoundaryIT`). A3 checks the producer's own body (lambdas included); it does not catch a use case built in an anonymous class or a private helper. The producer route is refused; a `TransactionRunner` port (A §6.2) stays the fallback only if the annotation exception is withdrawn.
- **A8.** The providers of `Clock` and the id ports (`IdGenerator`, `TokenGenerator`) in `app.rekord.application.config` may read the system clock and randomness; nobody else may.
- **A10.** A context may reach another only through `shared`, through a port the origin implements (shape b), or through a domain event a use case listens to (shape a, the marker is `DomainEvent`).
- **A13.** The helper that may build a `WebApplicationException` is not exempted yet; the §4.1 ticket writes that exemption when the helper exists.
- **A14.** Only `app.rekord.adapter.web.shared` sets a response status; the check covers `@ResponseStatus`, `Response`/`RestResponse` static factories and the status setters, matched with their subtypes so a call through an implementation type (`ResponseImpl`, `ContainerResponseContextImpl`, `Http1xServerResponse`) is caught too.

**Running it.** The suite runs in the ordinary `test` task and boots nothing. `ARCH_FITNESS_CMD` is therefore unset: there is no separate command and no tag.
