package app.rekord.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import java.lang.management.ManagementFactory;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TimeZoneIT {

    @Test
    void the_quarkus_test_jvm_runs_in_utc() {
        // Given: a running @QuarkusTest

        // When
        String zone = TimeZone.getDefault().getID();

        // Then
        assertThat(zone).isEqualTo("UTC");
        assertThat(ManagementFactory.getRuntimeMXBean().getInputArguments()).contains("-Duser.timezone=UTC");
    }
}
