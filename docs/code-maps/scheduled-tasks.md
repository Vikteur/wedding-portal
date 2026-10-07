# Code map: scheduled tasks

Decided in architecture-conventions §8.4 (line 597) and CT-21 (line 1106); not built yet: no scheduled job exists and `quarkus-scheduler` is not a dependency here yet.

- **Scheduler**: `quarkus-scheduler`, annotation `io.quarkus.scheduler.Scheduled`; nothing to enable.
- **Cadence**: a configuration property named by `cron` or `every`, never a literal.
- **Overlap**: `concurrentExecution = SKIP`, so a slow run is not overlapped.
- **Shape**: a thin entry point in `application` that delegates to a use case; idempotent, observable, catches and logs its exceptions so one bad run does not stop the schedule (FW-C-62).
- **Single instance** (18 C-27, docs/rewrite/analysis/18-framework-code-rulebook.md:554; A §8.4, CT-21): the deployment runs a single instance, so the skill's multi-instance guard is left out and added before a second instance runs. A lock table now would add a migration (a STOP) and guard nothing.
- **Batch-logging clause** (18 C-12, line 539): the skill's definition of done names an ArchUnit rule pairing the scheduling annotation with a batch-logging annotation that nothing defines. Not applicable while no job exists; when the first job is added, either drop the clause or define both the annotation and the rule here.
- A job that deletes personal data on a schedule is a STOP item before it ships (FW-C-61).
