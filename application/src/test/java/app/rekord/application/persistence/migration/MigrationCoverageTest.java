package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        // Then the entry says which class is missing
        assertThat(untested).containsExactly("99: V99MigrationIT does not exist");
    }

    @Test
    void a_test_that_declares_another_version_is_abstract_or_is_disabled_is_reported_with_its_reason(@TempDir Path dir)
            throws IOException {
        // Given four versions whose test classes are a good one, a wrong-version one, an abstract one and a disabled one
        for (String v : List.of("1", "2", "3", "4")) {
            Files.writeString(dir.resolve("V" + v + "__x.sql"), "select 1;\n");
        }
        Map<String, Class<?>> tests = Map.of(
                "1", GoodV1.class, "2", WrongVersion.class, "3", AbstractCheck.class, "4", DisabledCheck.class);

        // When
        List<String> untested = MigrationCoverage.untested(dir, v -> Optional.ofNullable(tests.get(v)));

        // Then only the good one counts as tested, and each other entry names its class and what is wrong with it
        assertThat(untested).containsExactly(
                "2: WrongVersion declares version \"5\" instead of \"2\"",
                "3: AbstractCheck is abstract",
                "4: DisabledCheck is @Disabled");
    }

    @Test
    void a_test_that_overrides_the_inherited_check_or_runs_only_under_a_condition_is_reported_with_its_reason(
            @TempDir Path dir) throws IOException {
        // Given two versions whose test classes replace the inherited check or skip it under a JUnit condition
        for (String v : List.of("5", "6")) {
            Files.writeString(dir.resolve("V" + v + "__x.sql"), "select 1;\n");
        }
        Map<String, Class<?>> tests = Map.of("5", OverridingCheck.class, "6", ConditionalCheck.class);

        // When
        List<String> untested = MigrationCoverage.untested(dir, v -> Optional.ofNullable(tests.get(v)));

        // Then neither counts as tested, and each entry says why
        assertThat(untested).containsExactly(
                "5: OverridingCheck overrides the inherited check",
                "6: ConditionalCheck carries @EnabledIfSystemProperty, a JUnit condition that can skip the check");
    }

    @Test
    void a_class_that_is_not_a_migration_schema_check_is_reported_with_its_reason(@TempDir Path dir) throws IOException {
        // Given a version whose class is named for it but does not extend MigrationSchemaCheck
        Files.writeString(dir.resolve("V7__x.sql"), "select 1;\n");

        // When
        List<String> untested = MigrationCoverage.untested(dir, v -> Optional.of(NotACheck.class));

        // Then
        assertThat(untested).containsExactly("7: NotACheck does not extend MigrationSchemaCheck");
    }

    @Test
    void a_test_class_whose_constructor_throws_fails_the_gate_with_the_cause(@TempDir Path dir) throws IOException {
        // Given a version whose test class cannot be instantiated
        Files.writeString(dir.resolve("V8__x.sql"), "select 1;\n");

        // When / Then the gate does not call it untested: it surfaces the constructor's own exception
        assertThatThrownBy(() -> MigrationCoverage.untested(dir, v -> Optional.of(ThrowingConstructor.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ThrowingConstructor")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("constructor failed on purpose");
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
    void the_version_of_a_file_name_is_what_the_gate_reads_from_it_and_nothing_for_a_name_it_refuses() {
        // Given names the gate accepts, with a hyphen, a space, a dot or an underscore in the version or description
        Map<String, String> accepted = Map.of(
                "V1__baseline.sql", "1",
                "V3__add-x.sql", "3",
                "V3__add x.sql", "3",
                "V3__a.b.sql", "3",
                "V1_1__x.sql", "1.1",
                "V1.2__x.sql", "1.2",
                "V10_2_3__x.sql", "10.2.3");

        // When / Then the version comes back with dots, as Flyway reads it
        accepted.forEach((name, version) ->
                assertThat(MigrationCoverage.versionOf(name)).as(name).contains(version));

        // And names that Flyway would not run as versioned migrations have none
        for (String name : List.of("V3_x.sql", "R__x.sql", "V__x.sql", "V1__x.txt", "V1__.sql", "v1__x.sql",
                "V1_1_x.sql", "notes.txt")) {
            assertThat(MigrationCoverage.versionOf(name)).as(name).isEmpty();
        }
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
    void a_subfolder_of_the_migration_folder_is_reported(@TempDir Path dir) throws IOException {
        // Given a V file in a subfolder, which Quarkus and Flyway both walk into
        Files.writeString(dir.resolve("V1__baseline.sql"), "-- baseline\n");
        Files.createDirectories(dir.resolve("v2"));
        Files.writeString(dir.resolve("v2/V2__nested.sql"), "select 1;\n");

        // When / Then the folder is reported, since no V<n>MigrationIT could be matched with the file in it
        assertThat(MigrationCoverage.unrecognised(dir)).as("unrecognised entries").containsExactly("v2/");
        assertThat(MigrationCoverage.versions(dir)).as("versions of the top level").containsExactly("1");
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
    void versions_are_sorted_in_flyways_order_including_dotted_and_timestamp_versions(@TempDir Path dir)
            throws IOException {
        // Given plain, dotted, underscored and timestamp versions, written out of order
        for (String file : List.of(
                "V10__ten.sql", "V20261008120000__timestamp.sql", "V2__two.sql", "V1.5__dotted.sql", "V1_10__underscored.sql")) {
            Files.writeString(dir.resolve(file), "select 1;\n");
        }

        // When / Then the order is Flyway's, and a version beyond int range does not break the gate
        assertThat(MigrationCoverage.versions(dir)).containsExactly("1.5", "1.10", "2", "10", "20261008120000");
    }

    @Test
    void test_resources_hold_no_db_migration_folder() {
        // Given the test resources, which share the classpath with the production migrations
        // When / Then a db/migration folder there would merge into classpath:db/migration
        assertThat(REPO_ROOT.resolve("application/src/test/resources/db/migration")).doesNotExist();
    }

    @Test
    void no_source_folder_but_the_application_main_resources_holds_a_db_migration_folder() {
        // Given the real repository, whose modules all reach classpath:db/migration
        // When / Then only the folder the gate reads holds migrations
        assertThat(MigrationCoverage.strayMigrationFolders(REPO_ROOT)).isEmpty();
    }

    @Test
    void a_db_migration_folder_in_another_module_or_source_set_is_reported(@TempDir Path root) throws IOException {
        // Given the gated folder, two stray ones, and build output that is not a source folder
        for (String folder : List.of(
                "application/src/main/resources/db/migration",
                "rekord-adapter/src/main/resources/db/migration",
                "application/src/test/resources/db/migration",
                "application/build/resources/main/db/migration")) {
            Files.createDirectories(root.resolve(folder));
        }

        // When / Then only the stray source folders are reported
        assertThat(MigrationCoverage.strayMigrationFolders(root)).containsExactly(
                "application/src/test/resources/db/migration", "rekord-adapter/src/main/resources/db/migration");
    }

    @Test
    void a_java_migration_folder_in_any_module_or_source_set_is_reported(@TempDir Path root) throws IOException {
        // Given Java-based migration folders, which Flyway reads from classpath:db/migration too, and build output
        for (String folder : List.of(
                "application/src/main/java/db/migration",
                "rekord-adapter/src/test/java/db/migration",
                "application/build/classes/java/main/db/migration",
                "application/src/main/java/app/rekord/db/migration")) {
            Files.createDirectories(root.resolve(folder));
        }

        // When / Then only the Java source folders named db/migration directly under src/<set>/java are reported
        assertThat(MigrationCoverage.strayMigrationFolders(root))
                .containsExactly("application/src/main/java/db/migration", "rekord-adapter/src/test/java/db/migration");
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

    private static final class NotACheck {}

    private static final class ThrowingConstructor extends MigrationSchemaCheck {
        ThrowingConstructor() {
            throw new IllegalArgumentException("constructor failed on purpose");
        }

        @Override
        protected String version() {
            return "8";
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
