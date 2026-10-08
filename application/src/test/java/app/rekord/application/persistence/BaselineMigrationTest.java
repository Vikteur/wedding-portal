package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BaselineMigrationTest {

    private static final Path MIGRATIONS =
            Path.of(System.getProperty("wedding.repoRoot")).resolve("application/src/main/resources/db/migration");
    private static final Path BASELINE = MIGRATIONS.resolve("V1__baseline.sql");
    private static final Pattern VERSIONED = Pattern.compile("V[0-9]+__[A-Za-z0-9_]+\\.sql");
    private static final Pattern STATEMENT =
            Pattern.compile("\\b(create|alter|drop|insert)\\b|;", Pattern.CASE_INSENSITIVE);

    @Test
    void db_migration_starts_with_v1_baseline_and_holds_only_versioned_migrations() throws IOException {
        // Given the migration directory
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<String> names = files.map(path -> path.getFileName().toString())
                    .sorted(Comparator.comparingInt(BaselineMigrationTest::version))
                    .toList();

            // Then the first file by version is the baseline, and every file is a versioned migration
            assertThat(names).first().isEqualTo("V1__baseline.sql");
            assertThat(names).allMatch(name -> VERSIONED.matcher(name).matches());
        }
    }

    private static int version(String fileName) {
        return Integer.parseInt(fileName.substring(1, fileName.indexOf("__")));
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
