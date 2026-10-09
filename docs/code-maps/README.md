# Code maps

One leaf per skill, `docs/code-maps/<skill name>.md`, named exactly after the skill folder under
`.claude/skills/`. The skill says *how* and holds no project nouns. The leaf shows it applied.

**Most leaves are worked examples, not project facts.** A leaf or exemplar with `kind: worked-example` in its
front matter shows the right structure for its skill: the layers, the classes and their roles, the call order, the
tests. Its names are pseudonymized placeholders: the project (`shop-backend`), the packages, the classes, the
methods, the parameters and the fields. Follow the structure, map every name to this repo's own, and never copy a
placeholder into code. `jvm-testing` is this repo's own leaf and carries real facts.

- When a skill needs a real project fact, write it into the skill's leaf. Then drop `kind: worked-example` and the
  note, or keep the example below a clearly separate "This repo" section.
- A decision and its reasoning go in `docs/memory.md`; the leaf links to it instead of repeating it.
- Any agent may correct a stale line. Never replace a worked example's structure with a thinner one.

The examples originate from a pseudonymized reference set. Shared material in this repo:
`exemplars/` (worked examples per artifact), `scaffold/` (mustache templates), [jvm-testing.worked-example] (the
worked example behind this repo's own [jvm-testing] leaf).

Leaves: [api-first-controller], [archunit-fitness], [bean-config-di], [clean-architecture], [clean-code], [code-generation], [design-review], [domain-dto-mapper], [domain-events], [domain-modeling], [domain-to-dto-mapper-tests], [exception-to-http], [flyway-migrations], [java], [jpa-entity-mapping], [junit-tests], [jvm-testing], [object-mother-builders], [openapi-rest-client-codegen], [persistence-repository], [resilience4j], [testcontainers], [usecase-bdd-spec], [usecase-orchestration], [validation-notification-result].

[api-first-controller]: api-first-controller.md
[archunit-fitness]: archunit-fitness.md
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
[flyway-migrations]: flyway-migrations.md
[java]: java.md
[jpa-entity-mapping]: jpa-entity-mapping.md
[junit-tests]: junit-tests.md
[jvm-testing]: jvm-testing.md
[jvm-testing.worked-example]: jvm-testing.worked-example.md
[object-mother-builders]: object-mother-builders.md
[openapi-rest-client-codegen]: openapi-rest-client-codegen.md
[persistence-repository]: persistence-repository.md
[resilience4j]: resilience4j.md
[testcontainers]: testcontainers.md
[usecase-bdd-spec]: usecase-bdd-spec.md
[usecase-orchestration]: usecase-orchestration.md
[validation-notification-result]: validation-notification-result.md
