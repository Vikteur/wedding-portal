package app.rekord.application.health;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StoppablePostgresTest {

    @Test
    void stopping_the_database_before_it_started_names_the_profile_that_starts_it() {
        // Given: no container was started in this JVM

        // Then
        assertThatThrownBy(StoppablePostgres::stopDatabase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ReadinessTestProfile");
    }
}
