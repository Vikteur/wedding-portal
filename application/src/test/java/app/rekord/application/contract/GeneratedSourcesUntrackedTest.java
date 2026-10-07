package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GeneratedSourcesUntrackedTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    private record Result(int exit, String out) {}

    private static Result git(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.waitFor(), out);
    }

    private static Path healthApi() throws IOException {
        Path generated = REPO_ROOT.resolve("rekord-adapter/build/generated/openapi");
        assertThat(generated).isDirectory();
        try (Stream<Path> walk = Files.walk(generated)) {
            return walk.filter(p -> p.getFileName().toString().equals("HealthApi.java"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("HealthApi.java was not generated"));
        }
    }

    @Test
    void the_generated_health_api_exists_but_git_tracks_no_build_output_or_api_package_file() throws Exception {
        // Given the generated HealthApi on disk
        assertThat(healthApi()).isRegularFile();

        // Then git lists nothing under a root or module build/ directory and nothing in a package directory app/rekord/api/
        assertThat(git("ls-files").out().lines())
                .noneMatch(line -> line.matches("([^/]+/)?build/.*"))
                .noneMatch(line -> line.contains("app/rekord/api/"));
    }

    @Test
    void git_ignores_the_generated_health_api() throws Exception {
        String relative = REPO_ROOT.relativize(healthApi()).toString().replace('\\', '/');
        assertThat(git("check-ignore", "-q", relative).exit()).isZero();
    }
}
