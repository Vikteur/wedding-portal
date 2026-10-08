package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** The comparison behind {@link FreshDatabase#assertStartedAfterThisJvm}, shown to fail without a database. */
class FreshDatabaseTest {

    @Test
    void a_server_older_than_this_jvm_is_refused() {
        // Given a server that has been up for 60 s and a JVM that has been up for 30 s
        // When / Then the server cannot have been started by this run
        assertThatThrownBy(() -> FreshDatabase.assertYounger(60_000, 30_000))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("started by this run");
    }

    @Test
    void a_server_as_old_as_this_jvm_is_refused() {
        // Given equal uptimes, which a server of an earlier run could never have beyond the JVM's own start
        // When / Then
        assertThatThrownBy(() -> FreshDatabase.assertYounger(30_000, 30_000))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("started by this run");
    }

    @Test
    void a_server_younger_than_this_jvm_is_accepted() {
        // Given a server that has been up for 5 s and a JVM that has been up for 30 s
        // When / Then
        assertThatCode(() -> FreshDatabase.assertYounger(5_000, 30_000)).doesNotThrowAnyException();
    }
}
