package app.rekord.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.domain.shared.error.ErrorCode;
import app.rekord.domain.shared.error.RejectedException;
import app.rekord.usecase.identity.BootstrapFirstAdminUseCase;
import app.rekord.usecase.identity.port.FirstAdminRepository;
import app.rekord.usecase.identity.port.NewFirstAdmin;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The log lines of the bootstrap as approved at the STOP gate (D4): ids and reasons only, never personal data. */
class FirstAdminBootstrapTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "made-up-pass-1234";
    private static final UUID BUSINESS = new UUID(0L, 1);
    private static final UUID ACCOUNT = new UUID(0L, 2);

    private record Settings(Optional<String> email, Optional<String> password, String displayName, String orgName)
            implements BootstrapSettings {}

    private static final class Repository implements FirstAdminRepository {
        boolean activeAdmin;
        RuntimeException failure;

        @Override
        public boolean hasActiveAdmin() {
            return activeAdmin;
        }

        @Override
        public void saveFirstAdmin(NewFirstAdmin admin) {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static final class Capture extends Handler {
        final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    private final Logger logger = Logger.getLogger(FirstAdminBootstrap.class.getName());
    private final Capture capture = new Capture();
    private final Repository repository = new Repository();

    @BeforeEach
    void capture() {
        capture.setLevel(Level.ALL);
        logger.addHandler(capture);
    }

    @AfterEach
    void release() {
        logger.removeHandler(capture);
    }

    private void start(String email, String password) {
        long[] next = {0};
        BootstrapFirstAdminUseCase useCase = new BootstrapFirstAdminUseCase(
                repository, value -> "scrypt$hash", () -> new UUID(0L, ++next[0]),
                Clock.fixed(Instant.parse("2027-06-12T10:00:00Z"), ZoneOffset.UTC));
        new FirstAdminBootstrap(
                        new Settings(Optional.ofNullable(email), Optional.ofNullable(password), "The planner",
                                "Rekord Match"),
                        useCase)
                .onStart(null);
    }

    /** The message as the console handler prints it, parameters filled in. */
    private static String text(LogRecord record) {
        return new PatternFormatter("%s").format(record);
    }

    private LogRecord theOnlyRecord(Level level) {
        assertThat(capture.records).hasSize(1);
        LogRecord record = capture.records.get(0);
        assertThat(record.getLevel()).isEqualTo(level);
        assertThat(record.getThrown()).isNull();
        String lower = text(record).toLowerCase(Locale.ROOT);
        assertThat(lower).doesNotContain(EMAIL).doesNotContain(PASSWORD).doesNotContain("the planner")
                .doesNotContain("rekord match");
        return record;
    }

    @Test
    void a_created_admin_is_one_info_line_with_the_account_and_business_ids() {
        start(EMAIL, PASSWORD);

        assertThat(text(theOnlyRecord(Level.INFO)))
                .isEqualTo("Created the first admin account " + ACCOUNT + " in business " + BUSINESS);
    }

    @Test
    void a_short_password_is_one_error_line_naming_the_setting() {
        start(EMAIL, "made-up-pas");

        assertThat(text(theOnlyRecord(Level.SEVERE))).isEqualTo("Refusing to bootstrap the first admin: the bootstrap"
                + " password (app.bootstrap.password) needs at least 12 characters. No admin was created.");
    }

    @Test
    void a_taken_address_is_one_error_line_naming_the_setting() {
        repository.failure = new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.DUPLICATE_USERNAME,
                "That address already belongs to an account.");

        start(EMAIL, PASSWORD);

        assertThat(text(theOnlyRecord(Level.SEVERE))).isEqualTo("Refusing to bootstrap the first admin: the bootstrap"
                + " address (app.bootstrap.email) already belongs to an account. No admin was created.");
    }

    @Test
    void a_taken_business_slug_is_one_error_line_naming_the_setting() {
        repository.failure = new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.DUPLICATE_NAME,
                "That business name is already taken.");

        start(EMAIL, PASSWORD);

        assertThat(text(theOnlyRecord(Level.SEVERE))).isEqualTo("Refusing to bootstrap the first admin: the business"
                + " name (app.bootstrap.org-name) gives a slug another business already has. No admin was created.");
    }

    @Test
    void another_refusal_is_one_error_line_naming_only_its_code() {
        repository.failure = new RejectedException(RejectedException.Kind.CONFLICT, ErrorCode.VALIDATION_FAILED,
                "Refused for " + EMAIL);

        start(EMAIL, PASSWORD);

        assertThat(text(theOnlyRecord(Level.SEVERE))).isEqualTo("Refusing to bootstrap the first admin: the database"
                + " refused it (VALIDATION_FAILED). No admin was created.");
    }

    @Test
    void no_settings_or_an_existing_admin_logs_nothing() {
        start(null, PASSWORD);
        start(EMAIL, "   ");
        repository.activeAdmin = true;
        start(EMAIL, PASSWORD);

        assertThat(capture.records).isEmpty();
    }

    @Test
    void a_failure_that_is_no_refusal_stops_the_start() {
        repository.failure = new IllegalStateException("database gone");

        assertThatThrownBy(() -> start(EMAIL, PASSWORD)).isInstanceOf(IllegalStateException.class);
        assertThat(capture.records).isEmpty();
    }
}
