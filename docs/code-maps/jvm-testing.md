# jvm-testing — wedding-portal

The project facts the `jvm-testing` skill applies to. No code yet (TASK-47 started over); these are the conventions
the first skeleton ticket sets up.

- **Test HTTP port 0.** Set `%test.quarkus.http.test-port=0` in `application.properties`, so parallel worktrees can
  run their test suites at the same time without a port clash. A fixed test port cost extra Gradle runs before the
  restart (umbrella `docs/archon/retro-synthesis.md`, row 11).
- **Line endings: check the blob, not the work tree.** `.gitattributes` forces LF for scripts, `*.json` and `*.md`.
  A check or a hash over a committed file reads `git show HEAD:<file>`, never the checked-out copy, which
  `core.autocrlf` may have turned to CRLF on Windows (row 12).
