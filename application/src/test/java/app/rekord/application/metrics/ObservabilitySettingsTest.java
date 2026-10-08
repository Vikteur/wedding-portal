package app.rekord.application.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * D1 and D2 of TASK-4.5 hold in every profile. The runtime tests run in the test profile only, so a profile-prefixed
 * line in application.properties could change production and leave them green.
 */
class ObservabilitySettingsTest {

    private static final String SUPPRESS = "quarkus.micrometer.binder.http-server.suppress4xx-errors";

    /** Keys whose extension default the readiness and metrics tests rely on: no profile may set them at all. */
    private static final List<String> NEVER_SET = List.of(
            "quarkus.datasource.health.enabled",
            "quarkus.management.enabled",
            "quarkus.http.non-application-root-path",
            "quarkus.smallrye-health.ui.always-include");

    private static Properties settings() throws IOException {
        Path file = Path.of(System.getProperty("wedding.repoRoot"))
                .resolve("application/src/main/resources/application.properties");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }

    private static boolean setIn(String name, String key) {
        return name.equals(key) || name.matches("%[\\w,-]+\\." + Pattern.quote(key));
    }

    @Test
    void untemplated_refusals_are_labelled_unknown_in_every_profile() throws IOException {
        // Given
        Properties settings = settings();

        // Then
        assertThat(settings.getProperty(SUPPRESS)).isEqualTo("true");
        assertThat(settings.stringPropertyNames())
                .noneMatch(name -> name.matches("%[\\w,-]+\\." + Pattern.quote(SUPPRESS)));
    }

    @Test
    void the_extension_defaults_the_readiness_and_metrics_tests_rely_on_are_never_overridden() throws IOException {
        // Given
        Set<String> names = settings().stringPropertyNames();

        // Then
        for (String key : NEVER_SET) {
            assertThat(names).as(key).noneMatch(name -> setIn(name, key));
        }
    }
}
