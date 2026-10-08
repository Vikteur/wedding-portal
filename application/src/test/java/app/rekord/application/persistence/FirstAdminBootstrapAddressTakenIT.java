package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.LogCapture;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * An address held by a disabled account and no active admin: the bootstrap is refused, the application starts, the
 * old row stays as it was, and the log line shows no address.
 */
@Tag("quarkus-unit-test")
@Singleton
@Unremovable
class FirstAdminBootstrapAddressTakenIT {

    private static final String PASSWORD = "made-up-pass-1234";

    static final MigratedDatabase DB = new MigratedDatabase("""
            insert into users (id, email, password_hash, display_name, status)
            values ('00000000-0000-0000-0000-000000000021', 'ADMIN@example.com', 'x', 'Former admin', 'DISABLED')""");

    @RegisterExtension
    static final QuarkusUnitTest app = BootstrapApplication.on(new QuarkusUnitTest(), FirstAdminBootstrapAddressTakenIT.class, DB)
            .overrideConfigKey("app.bootstrap.email", "admin@example.com")
            .overrideConfigKey("app.bootstrap.password", PASSWORD)
            .assertLogRecords(FirstAdminBootstrapAddressTakenIT::oneErrorWithoutPersonalData);

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    private static void oneErrorWithoutPersonalData(List<LogRecord> records) {
        List<LogRecord> bootstrap = MigratedDatabase.bootstrapRecords(records);
        assertThat(bootstrap).hasSize(1);
        assertThat(bootstrap.get(0).getLevel()).isEqualTo(Level.SEVERE);
        for (LogRecord record : records) {
            assertThat(LogCapture.text(record).toLowerCase(Locale.ROOT))
                    .doesNotContain("example.com")
                    .doesNotContain("admin@")
                    .doesNotContain(PASSWORD);
        }
    }

    @Test
    void the_start_succeeds_nothing_is_added_and_the_old_account_is_unchanged() {
        assertThat(DB.count("organizations")).isZero();
        assertThat(DB.count("memberships")).isZero();
        assertThat(DB.allRowsAsText()).isEqualTo(DB.rowsBeforeStart()).hasSize(1);
    }
}
