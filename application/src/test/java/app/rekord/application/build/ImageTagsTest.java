package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /** What the script reported: its exit code, its standard error and the GITHUB_OUTPUT file it wrote the tags to. */
    private record Outcome(int exit, String stderr, String output) {}

    private Outcome run(String repository, String sha) throws Exception {
        Path out = Files.writeString(tmp.resolve("github-output"), "");
        var result = ScriptRunner.in(REPO_ROOT).with("GITHUB_OUTPUT", out.toString()).run(SCRIPT, repository, sha);
        return new Outcome(result.exit(), result.stderr(), Files.readString(out));
    }
}
