# Code maps

The project facts a skill needs, one leaf per skill: `docs/code-maps/<skill name>.md`, named exactly after the skill
folder under `.claude/skills/`. The skill says *how* and holds no project nouns; the leaf says *what* for this repo
(P3): the modules, packages, classes, commands and conventions the skill applies to.

- Write a leaf when a skill first needs a project fact; extend it when the fact changes. Keep it short and current:
  describe the code as it is, never its history.
- A decision and its reasoning go in `docs/memory.md`; the leaf links to it instead of repeating it.
- The `pattern-scanner` agent writes and refreshes leaves from the code; any agent may correct a stale line.

Most leaves below are copied from `docs/code-maps-pseudonymized/` as a starting point: they describe a
different project (pseudonymized), not this repo yet. Rewrite a leaf for this repo the first time a skill uses it.
`jvm-testing` is this repo's own. Shared material from the same copy: `exemplars/`, `scaffold/`,
`_candidates.md` and `openapi-contract.example.yaml`.

Leaves: [aes-gcm-encryption], [api-first-controller], [archunit-fitness], [auth-context-facade], [bean-config-di], [clean-architecture], [clean-code], [code-generation], [design-review], [domain-dto-mapper], [domain-events], [domain-modeling], [domain-to-dto-mapper-tests], [exception-to-http], [failure-triage], [fhir-hapi-gateway], [flyway-migrations], [gateway-client-hygiene], [identifier-pseudonymization], [java], [jpa-entity-mapping], [junit-tests], [jvm-testing], [object-mother-builders], [openapi-rest-client-codegen], [persistence-repository], [resilience4j], [scheduled-tasks], [security-review], [soap-cxf-gateway], [spring-boot-slice-tests], [spring-caching], [testcontainers], [usecase-bdd-spec], [usecase-orchestration], [validation-notification-result], [wiremock-gateway-stubs].

[aes-gcm-encryption]: aes-gcm-encryption.md
[api-first-controller]: api-first-controller.md
[archunit-fitness]: archunit-fitness.md
[auth-context-facade]: auth-context-facade.md
[bean-config-di]: bean-config-di.md
[clean-architecture]: clean-architecture.md
[clean-code]: clean-code.md
[code-generation]: code-generation.md
[design-review]: design-review.md
[domain-dto-mapper]: domain-dto-mapper.md
[domain-events]: domain-events.md
[domain-modeling]: domain-modeling.md
[domain-to-dto-mapper-tests]: domain-to-dto-mapper-tests.md
[exception-to-http]: exception-to-http.md
[failure-triage]: failure-triage.md
[fhir-hapi-gateway]: fhir-hapi-gateway.md
[flyway-migrations]: flyway-migrations.md
[gateway-client-hygiene]: gateway-client-hygiene.md
[identifier-pseudonymization]: identifier-pseudonymization.md
[java]: java.md
[jpa-entity-mapping]: jpa-entity-mapping.md
[junit-tests]: junit-tests.md
[jvm-testing]: jvm-testing.md
[object-mother-builders]: object-mother-builders.md
[openapi-rest-client-codegen]: openapi-rest-client-codegen.md
[persistence-repository]: persistence-repository.md
[resilience4j]: resilience4j.md
[scheduled-tasks]: scheduled-tasks.md
[security-review]: security-review.md
[soap-cxf-gateway]: soap-cxf-gateway.md
[spring-boot-slice-tests]: spring-boot-slice-tests.md
[spring-caching]: spring-caching.md
[testcontainers]: testcontainers.md
[usecase-bdd-spec]: usecase-bdd-spec.md
[usecase-orchestration]: usecase-orchestration.md
[validation-notification-result]: validation-notification-result.md
[wiremock-gateway-stubs]: wiremock-gateway-stubs.md
