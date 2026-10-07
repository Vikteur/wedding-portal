package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ResourceTestTaggingTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path TEST_SOURCES = REPO_ROOT.resolve("application/src/test/java");
    private static final Pattern PROFILE =
            Pattern.compile("(?m)^[ \\t]*@TestProfile\\(ResourceTestProfile\\.class\\)[ \\t]*$");
    private static final Pattern TAG = Pattern.compile("(?m)^[ \\t]*@Tag\\(\"resource-test\"\\)[ \\t]*$");

    @Test
    void every_resource_profile_test_carries_the_resource_test_tag() throws IOException {
        // Given
        List<Path> withProfile = sourcesMatching(PROFILE);

        // When
        List<Path> withoutTag = withProfile.stream()
                .filter(path -> !matches(TAG, path))
                .toList();

        // Then
        assertThat(withProfile)
                .extracting(path -> path.getFileName().toString())
                .contains("HealthResourceIT.java", "ResourceTestProfileIT.java");
        assertThat(withoutTag)
                .as("tests in the resource profile without the resource-test tag (they would skip the tripwire)")
                .isEmpty();
    }

    @Test
    void every_resource_test_tagged_class_uses_the_resource_profile() throws IOException {
        // Given
        List<Path> withTag = sourcesMatching(TAG);

        // When
        List<Path> withoutProfile = withTag.stream()
                .filter(path -> !matches(PROFILE, path))
                .toList();

        // Then
        assertThat(withTag)
                .extracting(path -> path.getFileName().toString())
                .contains("HealthResourceIT.java", "ResourceTestProfileIT.java");
        assertThat(withoutProfile)
                .as("resource-test tagged tests without the resource profile (they would boot a datasource)")
                .isEmpty();
    }

    @Test
    void every_resource_test_tagged_class_ends_in_it() throws IOException {
        // Given
        List<Path> withTag = sourcesMatching(TAG);

        // When
        List<Path> notIt = withTag.stream()
                .filter(path -> !path.getFileName().toString().endsWith("IT.java"))
                .toList();

        // Then
        assertThat(withTag)
                .extracting(path -> path.getFileName().toString())
                .contains("HealthResourceIT.java", "ResourceTestProfileIT.java");
        assertThat(notIt)
                .as("resource-test tagged tests not ending in IT (resourceTest skips them, the fast set runs them"
                        + " without the tripwire)")
                .isEmpty();
    }

    private static List<Path> sourcesMatching(Pattern pattern) throws IOException {
        try (Stream<Path> files = Files.walk(TEST_SOURCES)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> matches(pattern, path))
                    .toList();
        }
    }

    private static boolean matches(Pattern pattern, Path path) {
        try {
            return pattern.matcher(Files.readString(path)).find();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
