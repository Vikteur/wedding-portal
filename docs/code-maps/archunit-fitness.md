# Code map: ArchUnit fitness

Decided in architecture-conventions §3 (line 199); not built yet: the suite `application/src/test/java/app/rekord/architecture` and rules A1 to A13 arrive with TASK-3.1. No ArchUnit dependency is in `gradle/libs.versions.toml` today.

**Package roots per layer** (A §2.3, line 156):

| Layer | Package root |
|---|---|
| domain | `app.rekord.domain..` (per context, plus `shared`) |
| use case | `app.rekord.usecase..` (ports in `..port`) |
| web adapter | `app.rekord.adapter.web..` |
| persistence adapter | `app.rekord.adapter.persistence..` |
| gateway | `app.rekord.gateway..` |
| application | `app.rekord.application..` |
| generated | `app.rekord.api..`, `app.rekord.api.model..` |

**The A3 exception (CT-11, A line 1096; 18 C-06, line 533).** A3 says `app.rekord.usecase..` depends only on `java..`, `app.rekord.domain..`, `app.rekord.usecase..` and the allow-list of §6.2. The written exception allows exactly two annotation types in `..usecase..`: `jakarta.transaction.Transactional` (the transaction at the use case, FW-C-14) and `jakarta.enterprise.context.ApplicationScoped` (ArC needs a bean for the interceptor). It is written into the rule itself, not into a frozen baseline. Fallback if the producer route loses the interceptor: a `TransactionRunner` port (A §6.2).

The suite runs in the ordinary `test` task, boots nothing, and every rule carries a `because(...)` message (A §3).
