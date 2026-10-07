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
                "catalog");
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

    private static void assertLeaf(Path relative, String... phrases) throws IOException {
        Path file = REPO_ROOT.resolve(relative);
        assertThat(file).as("the leaf %s", relative).isRegularFile();
        String text = Files.readString(file).toLowerCase(Locale.ROOT);
        for (String phrase : phrases) {
            assertThat(text).as("%s states '%s'", relative, phrase).contains(phrase.toLowerCase(Locale.ROOT));
        }
    }
}
