package app.rekord.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class ContainerTripwireTest {

    @Test
    void refuses_every_image_name() {
        // Given
        DockerImageName image = DockerImageName.parse("postgres:17-alpine");

        // When / Then
        assertThatThrownBy(() -> new ContainerTripwire().apply(image))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("postgres:17-alpine");
    }

    @Test
    void profile_turns_off_the_datasource_flyway_hibernate_and_dev_services() {
        // Given
        ResourceTestProfile profile = new ResourceTestProfile();

        // When
        Map<String, String> overrides = profile.getConfigOverrides();

        // Then
        assertThat(overrides)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "quarkus.devservices.enabled", "false",
                        "quarkus.datasource.devservices.enabled", "false",
                        "quarkus.datasource.active", "false",
                        "quarkus.flyway.active", "false",
                        "quarkus.flyway.migrate-at-start", "false",
                        "quarkus.hibernate-orm.active", "false"));
    }
}
