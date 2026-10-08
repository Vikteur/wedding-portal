package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.LogCapture;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Singleton;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** AC #2: an active admin already exists, so the settings create and change nothing. */
@Tag("quarkus-unit-test")
@Singleton
@Unremovable
class FirstAdminBootstrapAdminExistsIT {

    static final MigratedDatabase DB = new MigratedDatabase(MigratedDatabase.ACTIVE_ADMIN);

    @RegisterExtension
    static final QuarkusUnitTest app = BootstrapApplication.on(new QuarkusUnitTest(), FirstAdminBootstrapAdminExistsIT.class, DB)
            .overrideConfigKey("app.bootstrap.email", "admin@example.com")
            .overrideConfigKey("app.bootstrap.password", "made-up-pass-1234")
            .assertLogRecords(records -> {
                assertThat(MigratedDatabase.bootstrapRecords(records)).isEmpty();
                records.forEach(record -> assertThat(LogCapture.text(record)).doesNotContain("made-up-pass-1234"));
            });

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    @Test
    void every_row_is_the_same_after_the_start_and_nothing_was_added() {
        assertThat(DB.rowsBeforeStart()).hasSize(3);
        assertThat(DB.allRowsAsText()).isEqualTo(DB.rowsBeforeStart());
        assertThat(DB.count("organizations")).isEqualTo(1);
        assertThat(DB.count("users")).isEqualTo(1);
        assertThat(DB.count("memberships")).isEqualTo(1);
    }
}
