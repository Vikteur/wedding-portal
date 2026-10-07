# Code map: Conventional Commits

- **Type set**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`, plus `ci` (18 C-18, docs/rewrite/analysis/18-framework-code-rulebook.md:545). `ci` is added because the existing backend history already uses `ci:` subjects. `perf` and `style` are optional additions, not part of the set until agreed.
- **Shape**: `type(scope): imperative lower-case subject`, one logical change per commit; `!` or a `BREAKING CHANGE:` footer for incompatible changes (FW-C-79). The scope is optional.
- **Not enforced yet**: no commit-lint exists in this repository (18 C-18); the rule is followed by convention and reviewed on the PR.
