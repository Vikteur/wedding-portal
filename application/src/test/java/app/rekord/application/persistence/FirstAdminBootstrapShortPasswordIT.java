package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.LogCapture;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** AC #4: a password of 11 characters creates nothing and says why, without the password. */
@Tag("quarkus-unit-test")
@Singleton
@Unremovable
class FirstAdminBootstrapShortPasswordIT {

    private static final String PASSWORD = "made-up-pas";

    static final MigratedDatabase DB = new MigratedDatabase();

    @RegisterExtension
    static final QuarkusUnitTest app = BootstrapApplication.on(new QuarkusUnitTest(), FirstAdminBootstrapShortPasswordIT.class, DB)
            .overrideConfigKey("app.bootstrap.email", "admin@example.com")
            .overrideConfigKey("app.bootstrap.password", PASSWORD)
            .assertLogRecords(FirstAdminBootstrapShortPasswordIT::oneErrorWithoutThePassword);

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    private static void oneErrorWithoutThePassword(List<LogRecord> records) {
        List<LogRecord> bootstrap = MigratedDatabase.bootstrapRecords(records);
        assertThat(bootstrap).hasSize(1);
        assertThat(bootstrap.get(0).getLevel()).isEqualTo(Level.SEVERE);
        assertThat(LogCapture.text(bootstrap.get(0))).contains("at least 12 characters");
        records.forEach(record -> assertThat(LogCapture.text(record)).doesNotContain(PASSWORD));
    }

    @Test
    void the_start_succeeds_and_the_three_tables_are_empty() {
        assertThat(DB.count("organizations")).isZero();
        assertThat(DB.count("users")).isZero();
        assertThat(DB.count("memberships")).isZero();
    }
}
