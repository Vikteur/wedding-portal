package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** The gate: a V file without its {@code V<version>MigrationIT} fails the build (this class runs in {@code test}). */
class MigrationCoverageTest {

    private static final String PREFIX = "app.rekord.application.persistence.migration.";

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path MIGRATIONS = REPO_ROOT.resolve("application/src/main/resources/db/migration");

    @Test
    void every_versioned_migration_has_its_migration_test() {
        // Given the real migration folder
        // When
        List<String> versions = MigrationCoverage.versions(MIGRATIONS);

        // Then there are V files, and each has a working migration test
        assertThat(versions).isNotEmpty();
        assertThat(MigrationCoverage.untested(MIGRATIONS, MigrationCoverageTest::loadByName)).isEmpty();
        assertThat(MigrationCoverage.orphans(MIGRATIONS)).isEmpty();
        assertThat(MigrationCoverage.unrecognised(MIGRATIONS)).isEmpty();
    }

    @Test
    void a_versioned_migration_without_a_test_is_reported(@TempDir Path dir) throws IOException {
        // Given V1 and V99, and a lookup that only knows V1's test
        Files.writeString(dir.resolve("V1__baseline.sql"), "-- baseline\n");
        Files.writeString(dir.resolve("V99__untested.sql"), "select 1;\n");

        // When
        List<String> untested = MigrationCoverage.untested(dir, MigrationCoverageTest::loadByName);

        // Then
        assertThat(untested).containsExactly("99");
    }

    @Test
    void a_test_that_declares_another_version_is_abstract_or_is_disabled_is_reported(@TempDir Path dir)
            throws IOException {
        // Given four versions whose test classes are a good one, a wrong-version one, an abstract one and a disabled one
        for (String v : List.of("1", "2", "3", "4")) {
            Files.writeString(dir.resolve("V" + v + "__x.sql"), "select 1;\n");
        }
        Map<String, Class<?>> tests = Map.of(
                "1", GoodV1.class, "2", WrongVersion.class, "3", AbstractCheck.class, "4", DisabledCheck.class);

        // When
        List<String> untested = MigrationCoverage.untested(dir, v -> Optional.ofNullable(tests.get(v)));

        // Then only the good one counts as tested
        assertThat(untested).containsExactly("2", "3", "4");
    }

    @Test
    void a_test_that_overrides_the_inherited_check_or_runs_only_under_a_condition_is_reported(@TempDir Path dir)
            throws IOException {
        // Given two versions whose test classes replace the inherited check or skip it under a JUnit condition
        for (String v : List.of("5", "6")) {
            Files.writeString(dir.resolve("V" + v + "__x.sql"), "select 1;\n");
        }
        Map<String, Class<?>> tests = Map.of("5", OverridingCheck.class, "6", ConditionalCheck.class);

        // When
        List<String> untested = MigrationCoverage.untested(dir, v -> Optional.ofNullable(tests.get(v)));

        // Then neither counts as tested
        assertThat(untested).containsExactly("5", "6");
    }

    @Test
    void a_migration_test_without_its_v_file_is_reported(@TempDir Path dir) throws IOException {
        // Given a folder with V1 only and test classes named for V1 and V7
        Files.writeString(dir.resolve("V1__baseline.sql"), "-- baseline\n");

        // When
        List<String> orphans = MigrationCoverage.orphans(dir, List.of(PREFIX + "V1MigrationIT", PREFIX + "V7MigrationIT"));

        // Then
        assertThat(orphans).containsExactly(PREFIX + "V7MigrationIT");
    }

    @Test
    void a_file_that_is_not_a_versioned_migration_is_reported(@TempDir Path dir) throws IOException {
        // Given one good file and three that Flyway would not run as versioned migrations
        Files.writeString(dir.resolve("V1__baseline.sql"), "-- baseline\n");
        Files.writeString(dir.resolve("R__x.sql"), "select 1;\n");
        Files.writeString(dir.resolve("V1_baseline.sql"), "select 1;\n");
        Files.writeString(dir.resolve("notes.txt"), "notes\n");

        // When / Then
        assertThat(MigrationCoverage.unrecognised(dir)).containsExactlyInAnyOrder("R__x.sql", "V1_baseline.sql", "notes.txt");
    }

    @Test
    void versions_with_dots_or_underscores_map_to_one_test_class_name(@TempDir Path underscore, @TempDir Path dot)
            throws IOException {
        // Given the same version written both ways
        Files.writeString(underscore.resolve("V1_1__a.sql"), "select 1;\n");
        Files.writeString(dot.resolve("V1.1__a.sql"), "select 1;\n");

        // When / Then both give version 1.1 and the class V1_1MigrationIT
        assertThat(MigrationCoverage.versions(underscore)).containsExactly("1.1");
        assertThat(MigrationCoverage.versions(dot)).containsExactly("1.1");
        assertThat(MigrationCoverage.testClassName("1.1")).isEqualTo(PREFIX + "V1_1MigrationIT");
    }

    @Test
    void test_resources_hold_no_db_migration_folder() {
        // Given the test resources, which share the classpath with the production migrations
        // When / Then a db/migration folder there would merge into classpath:db/migration
        assertThat(REPO_ROOT.resolve("application/src/test/resources/db/migration")).doesNotExist();
    }

    private static Optional<Class<?>> loadByName(String version) {
        try {
            return Optional.of(Class.forName(MigrationCoverage.testClassName(version)));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    // Private, so JUnit never runs them.
    private static final class GoodV1 extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "1";
        }

        @Override
        protected SchemaSnapshot expected() {
            return SchemaSnapshot.empty();
        }
    }

    private static final class WrongVersion extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "5";
        }

        @Override
        protected SchemaSnapshot expected() {
            return SchemaSnapshot.empty();
        }
    }

    private abstract static class AbstractCheck extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "3";
        }
    }

    @Disabled
    private static final class DisabledCheck extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "4";
        }

        @Override
        protected SchemaSnapshot expected() {
            return SchemaSnapshot.empty();
        }
    }

    private static final class OverridingCheck extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "5";
        }

        @Override
        protected SchemaSnapshot expected() {
            return SchemaSnapshot.empty();
        }

        // Without @Test: JUnit runs nothing in its place.
        @Override
        void applies_the_history_up_to_and_including_its_migration_on_an_empty_container_and_finds_the_declared_schema() {}
    }

    @EnabledIfSystemProperty(named = "wedding.never.set", matches = "yes")
    private static final class ConditionalCheck extends MigrationSchemaCheck {
        @Override
        protected String version() {
            return "6";
        }

        @Override
        protected SchemaSnapshot expected() {
            return SchemaSnapshot.empty();
        }
    }
}
