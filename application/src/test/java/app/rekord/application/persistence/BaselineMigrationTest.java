package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BaselineMigrationTest {

    private static final Path MIGRATIONS =
            Path.of(System.getProperty("wedding.repoRoot")).resolve("application/src/main/resources/db/migration");
    private static final Path BASELINE = MIGRATIONS.resolve("V1__baseline.sql");
    private static final Pattern STATEMENT =
            Pattern.compile("\\b(create|alter|drop|insert)\\b|;", Pattern.CASE_INSENSITIVE);

    @Test
    void db_migration_holds_only_v1_baseline() throws IOException {
        // Given the migration directory
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            // Then it lists exactly the baseline
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("V1__baseline.sql");
        }
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
