package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class CodeMapLeavesTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path CODE_MAPS = Path.of("docs", "code-maps");

    @Test
    void security_review_names_the_governing_authority_for_each_surface() throws IOException {
        // 18 C-19: the code map names the authorities every security finding cites
        assertLeaf(CODE_MAPS.resolve("security-review.md"), "authorit", "surface");
    }

    @Test
    void jvm_testing_states_the_test_name_style_and_the_it_suffix_split() throws IOException {
        // 18 C-04 (style: snake-case behaviour sentence), architecture-conventions 13.1 and 13.2 (IT suffix split)
        assertLeaf(CODE_MAPS.resolve("jvm-testing.md"), "snake", "sentence", "IT", "integrationTest");
    }

    @Test
    void code_generation_states_the_open_api_generate_options() throws IOException {
        // architecture-conventions 4: generator, interfaceOnly, output under build/generated, version in the catalog
        assertLeaf(
                CODE_MAPS.resolve("code-generation.md"),
                "openApiGenerate",
                "jaxrs-spec",
                "interfaceOnly",
                "build/generated",
                "catalog",
                "7.25.0",
                "cleanupOutput",
                "contract.spec",
                "src/gen/java");
        // TASK-2.3 built the task: the leaf must not still say it is missing
        assertThat(Files.readString(REPO_ROOT.resolve(CODE_MAPS.resolve("code-generation.md"))).toLowerCase(Locale.ROOT))
                .as("code-generation.md no longer claims codegen is not built")
                .doesNotContain("not built yet");
    }

    @Test
    void spring_caching_states_the_cache_provider_decided_for_quarkus() throws IOException {
        // 18 C-28: Caffeine, today quarkus-cache
        assertLeaf(CODE_MAPS.resolve("spring-caching.md"), "quarkus-cache", "Caffeine");
    }

    @Test
    void java_states_the_language_level_build_tool_and_allowed_libraries() throws IOException {
        // architecture-conventions stack decision UD-2 (Java 25), UD-4 (Gradle)
        assertLeaf(CODE_MAPS.resolve("java.md"), "Java 25", "Gradle", "librar");
    }

    @Test
    void archunit_fitness_records_the_a3_exception_for_transactional_in_the_use_case() throws IOException {
        // 18 C-06, CT-11: @Transactional and @ApplicationScoped allowed in ..usecase.. as ArchUnit A3 exception
        assertLeaf(CODE_MAPS.resolve("archunit-fitness.md"), "A3", "CT-11", "Transactional", "ApplicationScoped");
    }

    @Test
    void archunit_fitness_records_the_a3_producer_refusal() throws IOException {
        assertLeaf(
                CODE_MAPS.resolve("archunit-fitness.md"), "@Produces", "PIN-AC-0448", "UseCaseTransactionBoundaryIT");
    }

    @Test
    void archunit_fitness_describes_the_built_suite() throws IOException {
        // TASK-3.1 built the suite: the leaf names where it lives, its store and the pinned version
        String catalog = Files.readString(REPO_ROOT.resolve("gradle/libs.versions.toml"));
        java.util.regex.Matcher pinned =
                java.util.regex.Pattern.compile("(?m)^archunit\\s*=\\s*\"([^\"]+)\"").matcher(catalog);
        assertThat(pinned.find()).as("the catalog pins archunit").isTrue();
        assertLeaf(
                CODE_MAPS.resolve("archunit-fitness.md"),
                "application/src/test/java/app/rekord/architecture",
                "A14",
                "archunit_store",
                "allowStoreUpdate",
                "ARCH_FITNESS_CMD",
                pinned.group(1));
        assertThat(Files.readString(REPO_ROOT.resolve(CODE_MAPS.resolve("archunit-fitness.md")))
                        .toLowerCase(Locale.ROOT))
                .as("archunit-fitness.md no longer claims the suite is not built")
                .doesNotContain("not built yet");
    }

    @Test
    void memory_records_the_archunit_pin_and_the_frozen_store() throws IOException {
        String memory = Files.readString(REPO_ROOT.resolve(Path.of("docs", "memory.md")));
        java.util.regex.Matcher heading =
                java.util.regex.Pattern.compile("(?m)^## .*TASK-3\\.1.*$").matcher(memory);
        assertThat(heading.find()).as("docs/memory.md has a TASK-3.1 section").isTrue();
        int end = memory.indexOf("\n## ", heading.end());
        String section = memory.substring(heading.end(), end < 0 ? memory.length() : end);

        assertThat(section).contains("PIN-AC-0230", "allowStoreUpdate");
        assertThat(section)
                .as("the TASK-3.1 memory entry names the CI run and head SHA that settled PIN-AC-0230")
                .contains("37652403500", "fd40198");
        assertThat(section)
                .as("the TASK-3.1 memory entry no longer carries the unfilled CI placeholder")
                .doesNotContain("filled in after CI");
    }

    @Test
    void conventional_commits_adds_ci_to_the_type_set() throws IOException {
        // 18 C-18: the type set gains `ci`
        assertLeaf(CODE_MAPS.resolve("conventional-commits.md"), "`ci`", "feat", "fix");
    }

    @Test
    void scheduled_tasks_states_the_batch_logging_clause_the_single_instance_note_and_the_scheduler() throws IOException {
        // 18 C-12 (batch logging), 18 C-27 (single instance), CT-21 and 8.4 (quarkus-scheduler, SKIP)
        assertLeaf(
                CODE_MAPS.resolve("scheduled-tasks.md"),
                "batch-logging",
                "single instance",
                "quarkus-scheduler",
                "concurrentExecution",
                "SKIP");
    }

    @Test
    void memory_records_that_the_domain_raises_events_and_the_use_case_publishes_them() throws IOException {
        // 18 C-01, architecture-conventions 5.4, CT-22
        assertLeaf(Path.of("docs", "memory.md"), "raised in the domain", "published by the use case");
    }

    @Test
    void memory_records_that_the_lint_commands_wait_for_the_formatter_decision() throws IOException {
        // TASK-3.4 AC #2: hooks.env ships with the three gate commands unset, and why
        assertLeaf(
                Path.of("docs", "memory.md"),
                "TASK-3.4",
                "LINT_FIX_CMD",
                "LINT_CHECK_CMD",
                "formatter decision",
                "ARCH_FITNESS_CMD",
                "jq");
    }

    @Test
    void jvm_testing_states_the_per_migration_test_its_gate_and_the_test_artifacts() throws IOException {
        // TASK-4.2 AC #2 and #3: every V file has its V<version>MigrationIT, the gate fails build without it, the row rollback
        assertLeaf(
                CODE_MAPS.resolve("jvm-testing.md"),
                "V<version>MigrationIT",
                "MigrationSchemaCheck",
                "MigrationCoverageTest",
                "migration-fixtures",
                "testArtifacts");
    }

    @Test
    void memory_records_the_migration_gate_and_the_row_rollback() throws IOException {
        // TASK-4.2: the decisions behind the gate, the fresh-container proof and the library-module test beans
        String memory = Files.readString(REPO_ROOT.resolve(Path.of("docs", "memory.md")));
        java.util.regex.Matcher heading =
                java.util.regex.Pattern.compile("(?m)^## .*TASK-4[.]2.*$").matcher(memory);
        assertThat(heading.find()).as("docs/memory.md has a TASK-4.2 section").isTrue();
        int end = memory.indexOf("\n## ", heading.end());
        String section = memory.substring(heading.end(), end < 0 ? memory.length() : end);

        assertThat(section).contains("MigrationCoverageTest", "RowRollbackIT", "probe_row", "testArtifacts", "FreshDatabase");
    }

    @Test
    void jvm_testing_states_what_else_the_migration_gate_and_the_snapshot_refuse() throws IOException {
        // TASK-4.2 review: the gate's stray folders and skipped checks, the snapshot's shared constraint names
        assertLeaf(
                CODE_MAPS.resolve("jvm-testing.md"),
                "strayMigrationFolders",
                "overrides the inherited check",
                "MigrationSchemaCheckIT",
                "table-specific constraint name");
    }

    @Test
    void jvm_testing_states_that_the_repository_files_tests_read_are_inputs_of_the_test_task() throws IOException {
        // TASK-40: an edit to a file a test reads runs the test again without --rerun
        assertLeaf(
                CODE_MAPS.resolve("jvm-testing.md"),
                "wedding.repoRoot",
                "inputs of the",
                "repoFiles",
                "contractFiles",
                "contract.spec",
                "smoke/pom.xml",
                "--rerun");
    }

    @Test
    void memory_supersedes_the_task_2_4_rerun_bullet_and_leaves_it_unedited() throws IOException {
        // Given the append-only memory file
        String memory = Files.readString(REPO_ROOT.resolve(Path.of("docs", "memory.md")));

        // When the TASK-40 section is cut out
        int start = memory.indexOf("## 2026-10-08 — TASK-40");
        if (start < 0) {
            start = memory.indexOf("TASK-40 ");
            start = memory.lastIndexOf("\n## ", start);
        }
        assertThat(start).as("a '## ... TASK-40 ...' section").isNotNegative();
        int next = memory.indexOf("\n## ", start + 1);
        String section = memory.substring(start, next < 0 ? memory.length() : next);

        // Then it supersedes the TASK-2.4 bullet and points to the code map
        assertThat(section).contains("Supersedes", "TASK-2.4", "--rerun", "docs/code-maps/jvm-testing.md");

        // And the old bullet is still there, word for word
        assertThat(memory)
                .contains("- To see a test that reads `ci.yml` or another repo file go red after a temporary edit,"
                        + " run it with `--rerun`: those files are not inputs of the Gradle `test` task, so a"
                        + " cached pass hides the edit.");
    }

    private static void assertLeaf(Path relative, String... phrases) throws IOException {
        Path file = REPO_ROOT.resolve(relative);
        assertThat(file).as("the leaf %s", relative).isRegularFile();
        String text = Files.readString(file).toLowerCase(Locale.ROOT);
        for (String phrase : phrases) {
            assertThat(text).as("%s states '%s'", relative, phrase).contains(phrase.toLowerCase(Locale.ROOT));
        }
    }
}
