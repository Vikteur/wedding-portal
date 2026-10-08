package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.error.LogCapture;
import app.rekord.application.security.Passwords;
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

/** AC #1, #5, #6: an empty database and both secret settings create the first business and its admin. */
@Tag("quarkus-unit-test")
@Singleton
@Unremovable
class FirstAdminBootstrapIT {

    private static final String PASSWORD = "made-up-pass-1234";

    static final MigratedDatabase DB = new MigratedDatabase();

    @RegisterExtension
    static final QuarkusUnitTest app = BootstrapApplication.on(new QuarkusUnitTest(), FirstAdminBootstrapIT.class, DB)
            .overrideConfigKey("app.bootstrap.email", " Admin@example.com ")
            .overrideConfigKey("app.bootstrap.password", PASSWORD)
            .assertLogRecords(FirstAdminBootstrapIT::theLogHoldsNoPersonalData);

    /**
     * Read before the container stops and kept in a system property: the log records are checked after the tests,
     * by the first copy of this class, which Quarkus does not share static fields with.
     */
    private static final String IDS = "first-admin-bootstrap-it.ids";

    @AfterAll
    static void stopDatabase() {
        System.setProperty(IDS, String.join(",",
                DB.column("select id::text from organizations union all select id::text from users")));
        DB.close();
    }

    private static void theLogHoldsNoPersonalData(List<LogRecord> records) {
        List<LogRecord> bootstrap = MigratedDatabase.bootstrapRecords(records);
        assertThat(bootstrap).hasSize(1);
        assertThat(bootstrap.get(0).getLevel()).isEqualTo(Level.INFO);
        String line = LogCapture.text(bootstrap.get(0));
        List<String> ids = List.of(System.getProperty(IDS, "").split(","));
        assertThat(ids).hasSize(2);
        assertThat(line).contains(ids.get(0)).contains(ids.get(1));
        for (LogRecord record : records) {
            String text = LogCapture.text(record).toLowerCase(Locale.ROOT);
            assertThat(text)
                    .doesNotContain(PASSWORD)
                    .doesNotContain("admin@example.com")
                    .doesNotContain("admin@")
                    .doesNotContain("@example.com")
                    .doesNotContain("the planner");
        }
    }

    @Test
    void the_business_the_account_and_the_admin_membership_are_created() {
        assertThat(DB.column("select name || '|' || timezone || '|' || slug from organizations"))
                .containsExactly("Rekord Match|Europe/Amsterdam|rekord-match");
        assertThat(DB.column("select email || '|' || display_name || '|' || status from users"))
                .containsExactly("admin@example.com|The planner|ACTIVE");
        assertThat(DB.column("select role || '|' || status from memberships")).containsExactly("ADMIN|ACTIVE");
        assertThat(DB.column("select count(*) from memberships m join users u on u.id = m.user_id"
                        + " join organizations o on o.id = m.org_id"))
                .containsExactly("1");
        assertThat(DB.column("select count(*) from users where password_changed_at is not null"))
                .containsExactly("1");
    }

    @Test
    void the_stored_hash_is_scrypt_and_verifies_the_password() {
        String hash = DB.column("select password_hash from users").get(0);

        assertThat(hash).matches("^scrypt[$]16384[$]8[$]1[$][0-9a-f]{32}[$][0-9a-f]{64}$");
        assertThat(new Passwords(count -> new byte[count]).verify(PASSWORD, hash)).isTrue();
        assertThat(new Passwords(count -> new byte[count]).verify(PASSWORD + "x", hash)).isFalse();
    }

    @Test
    void the_password_is_in_no_column_of_any_table() {
        assertThat(DB.allRowsAsText()).isNotEmpty().noneMatch(row -> row.contains(PASSWORD));
    }
}
