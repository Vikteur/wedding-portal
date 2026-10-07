package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class OpenApiGeneratorPinTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    private static String read(String relative) throws IOException {
        return Files.readString(REPO_ROOT.resolve(relative));
    }

    @Test
    void the_catalog_pins_the_generator_version_and_the_plugin_by_reference() throws IOException {
        // Given the version catalog
        String catalog = read("gradle/libs.versions.toml");

        // Then the generator version is 7.25.0 and the plugin takes it by reference
        assertThat(catalog).containsPattern(Pattern.compile("(?m)^openapi-generator\\s*=\\s*\"7\\.25\\.0\""));
        assertThat(catalog)
                .containsPattern(Pattern.compile("(?m)^openapi-generator\\s*=\\s*\\{[^}]*id\\s*=\\s*\"org\\.openapi\\.generator\""
                        + "[^}]*version\\.ref\\s*=\\s*\"openapi-generator\"[^}]*\\}"));
    }

    @Test
    void the_adapter_applies_the_plugin_by_catalog_alias() throws IOException {
        // Given the adapter build script
        String script = read("rekord-adapter/build.gradle.kts");

        // Then the plugin comes from the catalog
        assertThat(script).contains("alias(libs.plugins.openapi.generator)");
    }

    @Test
    void no_build_script_names_the_plugin_with_an_inline_version() throws IOException {
        // Given every Gradle build script
        List<Path> scripts;
        try (Stream<Path> walk = Files.walk(REPO_ROOT, 2)) {
            scripts = walk.filter(p -> p.getFileName().toString().endsWith(".gradle.kts"))
                    .toList();
        }

        // Then none applies org.openapi.generator with a literal version
        assertThat(scripts).isNotEmpty();
        for (Path script : scripts) {
            assertThat(Files.readString(script))
                    .as(script.toString())
                    .doesNotContainPattern("org\\.openapi\\.generator\"\\)?\\s*version");
        }
    }
}
