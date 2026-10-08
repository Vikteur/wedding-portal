package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.persistence.migration.MigrationCoverage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineMigrationTest {

    private static final Path MIGRATIONS =
            Path.of(System.getProperty("wedding.repoRoot")).resolve("application/src/main/resources/db/migration");
    private static final Path BASELINE = MIGRATIONS.resolve("V1__baseline.sql");
    private static final Pattern STATEMENT =
            Pattern.compile("\\b(create|alter|drop|insert)\\b|;", Pattern.CASE_INSENSITIVE);

    @Test
    void db_migration_starts_with_v1_baseline_and_holds_only_versioned_migrations() throws IOException {
        // Given the migration directory
        List<String> names = byVersion(MIGRATIONS);

        // Then the first file by version is the baseline, and every file is a versioned migration
        assertThat(names).first().isEqualTo("V1__baseline.sql");
        assertThat(names).allMatch(name -> MigrationCoverage.versionOf(name).isPresent());
    }

    @Test
    void a_dotted_or_underscored_version_that_the_migration_gate_accepts_sorts_by_its_flyway_version(@TempDir Path dir)
            throws IOException {
        // Given versioned migrations that MigrationCoverage accepts, V1.1 and V1_2 among them
        for (String name : List.of("V10__later.sql", "V2__identity.sql", "V1_2__fix.sql", "V1.1__fix.sql",
                "V1__baseline.sql")) {
            Files.writeString(dir.resolve(name), "-- x\n");
        }

        // When they are sorted by version
        List<String> names = byVersion(dir);

        // Then they sort as Flyway orders them, and each is a versioned migration
        assertThat(names).containsExactly(
                "V1__baseline.sql", "V1.1__fix.sql", "V1_2__fix.sql", "V2__identity.sql", "V10__later.sql");
        assertThat(names).allMatch(name -> MigrationCoverage.versionOf(name).isPresent());
    }

    @Test
    void a_hyphenated_description_that_the_migration_gate_accepts_is_a_versioned_migration(@TempDir Path dir)
            throws IOException {
        // Given V3__add-x.sql, whose description holds a hyphen: MigrationCoverage accepts any description (.+)
        for (String name : List.of("V3__add-x.sql", "V1__baseline.sql")) {
            Files.writeString(dir.resolve(name), "-- x\n");
        }

        // When they are sorted by version
        List<String> names = byVersion(dir);

        // Then the hyphenated file is accepted and sorts after the baseline
        assertThat(names).containsExactly("V1__baseline.sql", "V3__add-x.sql");
        assertThat(names).allMatch(name -> MigrationCoverage.versionOf(name).isPresent());
    }

    private static List<String> byVersion(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(path -> path.getFileName().toString())
                    .sorted(Comparator.comparing(BaselineMigrationTest::version))
                    .toList();
        }
    }

    private static MigrationVersion version(String fileName) {
        // The version comes from the migration gate itself, so what it accepts and what this test sorts cannot drift
        return MigrationVersion.fromVersion(MigrationCoverage.versionOf(fileName)
                .orElseThrow(() -> new AssertionError(fileName + " is not a versioned migration")));
    }

    @Test
    void v1_baseline_holds_only_comments() throws IOException {
        // Given the baseline lines
        List<String> lines = Files.readAllLines(BASELINE);

        // Then every non-blank line is a comment and no statement sits outside one
        assertThat(lines.stream().filter(line -> !line.isBlank()))
                .isNotEmpty()
                .allSatisfy(line -> assertThat(line).startsWith("--"));
        assertThat(lines.stream().filter(line -> !line.startsWith("--")))
                .noneMatch(line -> STATEMENT.matcher(line).find());
    }

    @Test
    void v1_baseline_has_lf_endings() throws IOException {
        // Given the baseline bytes
        byte[] bytes = Files.readAllBytes(BASELINE);

        // Then there is no carriage return
        assertThat(bytes).doesNotContain((byte) '\r');
    }
}
