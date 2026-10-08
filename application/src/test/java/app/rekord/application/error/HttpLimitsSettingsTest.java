package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HttpLimitsSettingsTest {

    private static final String KEY = "quarkus.http.limits.max-body-size";

    private static Properties settings() throws IOException {
        Path file = Path.of(System.getProperty("wedding.repoRoot"))
                .resolve("application/src/main/resources/application.properties");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }

    @Test
    void the_request_body_limit_is_explicit_at_10240K() throws IOException {
        assertThat(settings().getProperty(KEY)).isEqualTo("10240K");
    }

    @Test
    void no_profile_overrides_the_request_body_limit() throws IOException {
        assertThat(settings().stringPropertyNames())
                .noneMatch(name -> name.matches("%[\\w-]+\\." + Pattern.quote(KEY)));
    }
}
