package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The image check reads {@code flyway_schema_history} of the started image. It expects one successful row per
 * versioned migration, not a literal count that the next V file would break on the first main run.
 */
class ImageCheckMigrationCountTest {

    private static final long TIMEOUT_SECONDS = 30;
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
    void the_migration_count_line_counts_the_v_files_of_the_migration_folder(@TempDir Path tempDir) throws Exception {
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
        Path scriptFile = Files.writeString(tempDir.resolve("migration-count.sh"), script);
        // Output goes to a file too, so a bash that hangs cannot block the read of its output
        Path outputFile = tempDir.resolve("migration-count.out");
        String bash = bashExecutable();
        Process process = new ProcessBuilder(bash, scriptFile.toString().replace('\\', '/'))
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile())
                .start();
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
        assertThat(finished).as("%s finished within %d seconds", bash, TIMEOUT_SECONDS).isTrue();
        String output = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
        assertThat(process.exitValue()).as(output).isZero();

        // Then it prints the number of V*__*.sql files that Java lists
        assertThat(versioned).isGreaterThanOrEqualTo(2);
        assertThat(output).isEqualTo(String.valueOf(versioned));
    }

    /**
     * Plain {@code bash} on Linux and macOS. On Windows a bare {@code bash} can be the launcher of WSL, which does not
     * read a Windows path, so the Git Bash that sits with {@code git.exe} is used. The test fails, and is never skipped,
     * when there is none.
     */
    private static String bashExecutable() {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            return "bash";
        }
        List<Path> candidates = new ArrayList<>();
        for (String dir : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            try {
                Path git = Path.of(dir, "git.exe");
                if (Files.isRegularFile(git)) {
                    // git.exe is in <root>\cmd, <root>\bin or <root>\mingw64\bin of Git for Windows; bash.exe is in
                    // <root>\bin
                    Path up = git.getParent();
                    for (int level = 0; level < 3 && up != null; level++, up = up.getParent()) {
                        candidates.add(up.resolve("bin").resolve("bash.exe"));
                    }
                }
            } catch (InvalidPathException notAPath) {
                // an entry of the PATH that is no path has no git.exe
            }
        }
        candidates.add(Path.of("C:\\Program Files\\Git\\bin\\bash.exe"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Git Bash not found next to git.exe on the PATH: " + candidates))
                .toString();
    }
}
