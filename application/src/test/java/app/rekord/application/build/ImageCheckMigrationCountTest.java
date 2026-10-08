package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The image check reads {@code flyway_schema_history} of the started image. It expects one successful row per
 * versioned migration, not a literal count that the next V file would break on the first main run.
 */
class ImageCheckMigrationCountTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path SCRIPT = REPO_ROOT.resolve(".github/scripts/image-check.sh");
    private static final Path MIGRATIONS = REPO_ROOT.resolve("application/src/main/resources/db/migration");

    @Test
    void the_flyway_check_expects_as_many_rows_as_there_are_versioned_migrations() throws IOException {
        // Given the image check
        List<String> lines = Files.readAllLines(SCRIPT);

        // Then it no longer expects a literal count of rows
        assertThat(lines).noneMatch(line -> line.contains("expect \"successful flyway_schema_history rows\" \"1\""));

        // And its expectation is the variable that the migrations= line assigns
        assertThat(lines).anyMatch(line -> line.startsWith("migrations=\""));
        assertThat(lines).anyMatch(line -> line.contains("expect \"successful flyway_schema_history rows\" \"$migrations\""));
    }

    @Test
    void the_migration_count_line_counts_the_v_files_of_the_migration_folder() throws Exception {
        // Given the line that assigns migrations
        String line = Files.readAllLines(SCRIPT).stream()
                .filter(l -> l.startsWith("migrations="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("image-check.sh has no migrations= line"));
        long versioned;
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            versioned = files.map(p -> p.getFileName().toString())
                    .filter(name -> name.matches("V.*__.*\\.sql"))
                    .count();
        }

        // When it runs under bash with repo_root set to the repo
        String script = "repo_root='" + REPO_ROOT.toString().replace('\\', '/') + "'\n" + line + "\necho \"$migrations\"\n";
        // Through a file: Windows argument quoting would mangle the quotes of a -c script
        Path scriptFile = Files.createTempFile("migration-count", ".sh");
        Files.writeString(scriptFile, script);
        Process process = new ProcessBuilder(bashExecutable(), scriptFile.toString().replace('\\', '/'))
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        Files.delete(scriptFile);
        assertThat(process.waitFor()).as(output).isZero();

        // Then it prints the number of V*__*.sql files that Java lists
        assertThat(versioned).isGreaterThanOrEqualTo(2);
        assertThat(output).isEqualTo(String.valueOf(versioned));
    }

    private static String bashExecutable() {
        Path gitBash = Path.of("C:\\Program Files\\Git\\bin\\bash.exe");
        if (Files.exists(gitBash)) {
            return gitBash.toString();
        }
        return "bash";
    }
}
