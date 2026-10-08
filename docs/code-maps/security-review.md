# Code map: security review

Every `security-review` finding cites a named authority from this list (18 C-19, docs/rewrite/analysis/18-framework-code-rulebook.md:546). The surface-to-authority table below is the part the generic skill points at.

| Surface | Governing authority |
|---|---|
| Authentication, authorization, access matrix | architecture-conventions §10 and §10.1 (STOP-gated; cookie mechanism and `ActorResolver` placement; ArchUnit A7); route guard `RouteGuardTest` (BR-OPS-22: every implemented `app.rekord.api` route outside the seven-entry allow-list carries `@Authenticated`, `@RolesAllowed`, `@PermitAll` or `@DenyAll` on the implementing method or class); the extension `io.quarkus:quarkus-security` makes Quarkus REST enforce those annotations at runtime; no `@PermitAll` in production (D2). Not seen: routes that are not JAX-RS classes in the class directories of the modules (a Vert.x `@Route` or `Router` handler, a route from an extension or a dependency jar, a static resource), and which roles an annotation names; see the TASK-6.1 entry of `docs/memory.md` |
| Personal data in logs, identifier masking, audit trail | architecture-conventions §12 and §12.1 (STOP-gated; ArchUnit A8 for `SecureRandom` / `UUID.randomUUID()` placement); `docs/memory.md` (TASK-7.2: `logServerErrorDetail=false` keeps refused values out of application logs and exception messages; never log a `ServerErrorMessage`; the database server log needs `log_error_verbosity = terse`, owned by TASK-30.5) |
| Secrets and production configuration | `.claude/CLAUDE.md` STOP list; `docs/memory.md` (TASK-4.1: `%prod` reads `DB_URL`, `DB_USER`, `DB_PASSWORD` from the environment, no default password) |
| Transport and CI | `docs/memory.md` (pinned actions, read-only contract token, `pull_request_target` never used) |
| Dependency bumps | `gradle/libs.versions.toml` (all versions pinned in the catalog) |

Not decided yet (open in 18 C-19, line 546): which fields are personal identifiers and whether any is encrypted at rest. Until that is decided, encryption is out of scope and any such change is a STOP item.

Sources: architecture-conventions.md §10 (line 658), §12 (line 799); 18 C-19 (line 546).
