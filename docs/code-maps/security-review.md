# Code map: security review

Every `security-review` finding cites a named authority from this list (18 C-19, docs/rewrite/analysis/18-framework-code-rulebook.md:546). The surface-to-authority table below is the part the generic skill points at.

| Surface | Governing authority |
|---|---|
| Authentication, authorization, access matrix | architecture-conventions §10 and §10.1 (STOP-gated; cookie mechanism and `ActorResolver` placement; ArchUnit A7) |
| Personal data in logs, identifier masking, audit trail | architecture-conventions §12 and §12.1 (STOP-gated; ArchUnit A8 for `SecureRandom` / `UUID.randomUUID()` placement); `docs/memory.md` (TASK-7.2: `logServerErrorDetail=false` keeps refused values out of application logs and exception messages; never log a `ServerErrorMessage`; the database server log needs `log_error_verbosity = terse`) |
| Secrets and production configuration | `.claude/CLAUDE.md` STOP list; `docs/memory.md` (TASK-4.1: `%prod` reads `DB_URL`, `DB_USER`, `DB_PASSWORD` from the environment, no default password) |
| Transport and CI | `docs/memory.md` (pinned actions, read-only contract token, `pull_request_target` never used) |
| Dependency bumps | `gradle/libs.versions.toml` (all versions pinned in the catalog) |

Not decided yet (open in 18 C-19, line 546): which fields are personal identifiers and whether any is encrypted at rest. Until that is decided, encryption is out of scope and any such change is a STOP item.

Sources: architecture-conventions.md §10 (line 658), §12 (line 799); 18 C-19 (line 546).
