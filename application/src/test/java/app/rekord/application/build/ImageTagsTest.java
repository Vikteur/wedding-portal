package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageTagsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final String SCRIPT = ".github/scripts/image-tags.sh";
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    @TempDir
    Path tmp;

    @Test
    void tags_are_the_full_commit_sha_and_latest_under_the_lowercased_repository() throws Exception {
        // Given / When
        var result = run("Vikteur/Wedding-Portal", SHA);

        // Then
        assertThat(result.exit()).as(result.stderr()).isZero();
        assertThat(result.output().lines().toList())
                .contains(
                        "name=ghcr.io/vikteur/wedding-portal",
                        "sha=ghcr.io/vikteur/wedding-portal:" + SHA,
                        "latest=ghcr.io/vikteur/wedding-portal:latest");
        assertThat(result.output()).doesNotContainPattern("[A-Z]");
    }

    @Test
    void a_short_or_uppercase_or_non_hex_sha_is_refused() throws Exception {
        for (String sha : List.of("abc1234", SHA.toUpperCase(), SHA + "a", "main")) {
            // When
            var result = run("Vikteur/wedding-portal", sha);

            // Then
            assertThat(result.exit()).as(sha).isNotZero();
            assertThat(result.stderr()).as(sha).contains("FAIL:").contains("40 lowercase hex");
            assertThat(result.output()).as(sha).isEmpty();
        }
    }

    @Test
    void a_repository_that_is_not_owner_slash_name_is_refused() throws Exception {
        for (String repository : List.of("wedding-portal", "a/b/c", "")) {
            // When
            var result = run(repository, SHA);

            // Then
            assertThat(result.exit()).as(repository).isNotZero();
            assertThat(result.stderr()).as(repository).contains("FAIL:").contains("owner/name");
            assertThat(result.output()).as(repository).isEmpty();
        }
    }

    @Test
    void the_script_is_strict_and_never_reads_a_token() throws IOException {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(SCRIPT));

        // When / Then
        assertThat(script).contains("set -euo pipefail");
        assertThat(script).doesNotContain("token", "TOKEN", "docker login", "secrets");
    }

    private record Result(int exit, String stderr, String output) {}

    private Result run(String repository, String sha) throws Exception {
        Path out = Files.writeString(tmp.resolve("github-output"), "");
        var command = new ArrayList<String>();
        command.add(bashExecutable());
        command.addAll(List.of(SCRIPT, repository, sha));
        var builder = new ProcessBuilder(command).directory(REPO_ROOT.toFile());
        builder.environment().keySet().removeIf(key -> key.startsWith("GITHUB_"));
        builder.environment().put("GITHUB_OUTPUT", out.toString());
        Path err = Files.createTempFile(tmp, "stderr", ".txt");
        builder.redirectError(err.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        int exit = builder.start().waitFor();
        return new Result(exit, Files.readString(err), Files.readString(out));
    }

    private static String bashExecutable() {
        Path gitBash = Path.of("C:\\Program Files\\Git\\bin\\bash.exe");
        if (Files.exists(gitBash)) {
            return gitBash.toString();
        }
        Assumptions.assumeFalse(
                System.getProperty("os.name").toLowerCase().contains("win"), "no Git Bash available on Windows");
        return "bash";
    }
}
