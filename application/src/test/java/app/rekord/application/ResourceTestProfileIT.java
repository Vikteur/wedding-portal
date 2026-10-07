package app.rekord.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class ResourceTestProfileIT {

    @Test
    void starts_with_the_container_tripwire_armed() {
        // Given: a Quarkus boot in the resource-test profile

        // When
        String substitutor = System.getenv("TESTCONTAINERS_IMAGE_SUBSTITUTOR");

        // Then: a container attempt would have failed the boot
        assertThat(substitutor).isEqualTo(ContainerTripwire.class.getName());
    }

    @Test
    void the_default_datasource_is_inactive_and_has_no_jdbc_url() {
        // Given: the resource-test profile

        // When
        boolean usable = isUsable(AgroalDataSource.class);

        // Then
        assertThat(usable).isFalse();
        assertThat(ConfigProvider.getConfig().getOptionalValue("quarkus.datasource.jdbc.url", String.class))
                .isEmpty();
    }

    @Test
    void flyway_and_hibernate_do_not_run() {
        // Given: the resource-test profile

        // When
        boolean flyway = isUsable(Flyway.class);
        boolean sessionFactory = isUsable(SessionFactory.class);

        // Then
        assertThat(flyway).isFalse();
        assertThat(sessionFactory).isFalse();
    }

    private static boolean isUsable(Class<?> type) {
        try {
            var handle = Arc.container().instance(type);
            return handle.isAvailable() && handle.getBean().isActive();
        } catch (InactiveBeanException inactive) {
            return false;
        }
    }
}
