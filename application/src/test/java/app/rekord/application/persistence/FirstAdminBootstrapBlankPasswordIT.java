package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** AC #3: an address with a blank password counts as not configured. */
@Tag("quarkus-unit-test")
@Singleton
@Unremovable
class FirstAdminBootstrapBlankPasswordIT {

    static final MigratedDatabase DB = new MigratedDatabase();

    @RegisterExtension
    static final QuarkusUnitTest app = BootstrapApplication.on(new QuarkusUnitTest(), FirstAdminBootstrapBlankPasswordIT.class, DB)
            .overrideConfigKey("app.bootstrap.email", "admin@example.com")
            .overrideConfigKey("app.bootstrap.password", "   ")
            .assertLogRecords(records -> assertThat(MigratedDatabase.bootstrapRecords(records)).isEmpty());

    @AfterAll
    static void stopDatabase() {
        DB.close();
    }

    @Test
    void the_start_succeeds_and_the_three_tables_are_empty() {
        assertThat(DB.count("organizations")).isZero();
        assertThat(DB.count("users")).isZero();
        assertThat(DB.count("memberships")).isZero();
    }
}
