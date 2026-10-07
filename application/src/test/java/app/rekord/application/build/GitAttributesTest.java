package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GitAttributesTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Set<String> SKIPPED_DIRS = Set.of(".git", ".gradle", ".idea", ".kotlin");
    private static final Set<String> SKIPPED_OUTPUT_DIRS = Set.of("build", "out");

    @Test
    void sql_files_are_checked_out_with_lf_endings() throws IOException {
        // Given the repository attributes file
        List<String> rules = Files.readAllLines(REPO_ROOT.resolve(".gitattributes")).stream()
                .map(line -> line.trim().replaceAll("\\s+", " "))
                .toList();

        // Then SQL files are pinned to LF
        assertThat(rules).contains("*.sql text eol=lf");
    }

    @Test
    void git_resolves_eol_lf_for_a_migration_file() throws Exception {
        // Given a migration path, which need not exist because git applies attributes by path
        // When git resolves the eol attribute
        Process process = new ProcessBuilder(
                        "git",
                        "check-attr",
                        "eol",
                        "--",
                        "application/src/main/resources/db/migration/V1__baseline.sql")
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        int exit = process.waitFor();

        // Then it is lf
        assertThat(exit).isZero();
        assertThat(output).endsWith("eol: lf");
    }

    @Test
    void no_sql_file_in_the_repo_contains_a_carriage_return() throws IOException {
        // Given every SQL file in the repo
        try (Stream<Path> files = Files.walk(REPO_ROOT)) {
            List<Path> offending = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".sql"))
                    .filter(GitAttributesTest::isTracked)
                    .filter(GitAttributesTest::hasCarriageReturn)
                    .toList();

            // Then none contains a CR byte
            assertThat(offending).isEmpty();
        }
    }

    private static boolean isTracked(Path file) {
        Path relative = REPO_ROOT.relativize(file);
        for (int i = 0; i < relative.getNameCount() - 1; i++) {
            String part = relative.getName(i).toString();
            if (SKIPPED_DIRS.contains(part)
                    || (i <= 1 && SKIPPED_OUTPUT_DIRS.contains(part))
                    || (i == 0 && part.equals("contract"))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCarriageReturn(Path file) {
        try {
            for (byte b : Files.readAllBytes(file)) {
                if (b == '\r') {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
