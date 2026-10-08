package app.rekord.application.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.persistence.identity.OrganizationEntity;
import app.rekord.adapter.persistence.identity.SessionEntity;
import app.rekord.adapter.persistence.identity.UserEntity;
import app.rekord.application.error.LogCapture;
import app.rekord.application.persistence.AbstractRepositoryTest;
import app.rekord.application.persistence.Refusals;
import io.quarkus.test.junit.QuarkusTest;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

/**
 * stop-crypto-logging (BR-ID-09): a refused identity row puts neither its address, its password hash nor its token
 * hash into a log line or an exception message.
 */
@QuarkusTest
class IdentityRefusalLoggingIT extends AbstractRepositoryTest {

    @Test
    void a_refused_duplicate_address_appears_in_no_log_line_or_exception_message() {
        // Given a stored user
        inNewTransaction(() -> em.persist(IdentityRows.user(1, "planner@example.com", "Pat Planner")));

        // When the same address in other case is refused
        UserEntity duplicate = IdentityRows.user(2, "Planner@example.com", "Pat Planner");
        Outcome outcome = capturing(() -> em.persist(duplicate));

        // Then the refusal names its constraint and was logged, and the address is in no record and no message
        outcome.assertRefusedOn("ux_users_email");
        outcome.assertNoneContains("example.com");
        outcome.assertNoneContains("Pat Planner");
    }

    @Test
    void a_refused_user_row_shows_neither_its_password_hash_nor_its_address() {
        // Given a user row whose status breaks the status check
        UserEntity user = IdentityRows.user(1, "planner@example.com", "Pat Planner");
        user.setStatus("NOPE");

        // When it is refused
        Outcome outcome = capturing(() -> em.persist(user));

        // Then the refusal names its constraint and was logged, and neither the hash nor the address is in any
        // record or message
        outcome.assertRefusedOn("users_status_check");
        outcome.assertNoneContains(IdentityRows.PASSWORD_HASH);
        outcome.assertNoneContains("example.com");
    }

    @Test
    void a_refused_session_row_shows_no_token_hash() {
        // Given an organisation, a user, and a user session that also names a portal (breaks the subject check)
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
        });
        SessionEntity session = IdentityRows.userSession(1, org, user);
        session.setPortalId(IdentityRows.id(900));

        // When it is refused
        Outcome outcome = capturing(() -> em.persist(session));

        // Then the refusal names its constraint and was logged, and the token hash is in no record or message,
        // in hex or in PostgreSQL's \x form
        outcome.assertRefusedOn("ck_sessions_subject");
        outcome.assertNoTokenHash(IdentityRows.tokenHash(1));
    }

    @Test
    void a_refused_duplicate_token_hash_shows_the_hash_in_no_record_and_no_message() {
        // Given an organisation, a user and a stored session with a token hash
        OrganizationEntity org = IdentityRows.organization();
        UserEntity user = IdentityRows.planner();
        inNewTransaction(() -> {
            em.persist(org);
            em.persist(user);
            em.persist(IdentityRows.userSession(1, org, user));
        });

        // When another session with the same token hash is refused on the unique index
        SessionEntity duplicate = IdentityRows.userSession(2, org, user);
        duplicate.setTokenHash(IdentityRows.tokenHash(1));
        Outcome outcome = capturing(() -> em.persist(duplicate));

        // Then the index is named, and the hash is in no record or message, in hex or in PostgreSQL's \x form
        outcome.assertRefusedOn("ux_sessions_token");
        outcome.assertNoTokenHash(IdentityRows.tokenHash(1));
    }

    /** Runs the action in a new transaction under log capture; the action must be refused. */
    private Outcome capturing(Runnable action) {
        LogCapture capture = new LogCapture();
        capture.start();
        Throwable refused;
        try {
            refused = refusedWith(action);
        } finally {
            capture.stop();
        }
        return new Outcome(refused, capture.records());
    }

    private record Outcome(Throwable refused, List<LogRecord> records) {

        /**
         * The refusal names the expected constraint (read from the PSQLException in its cause chain, so a renamed
         * constraint fails here and not only by substring), and a captured record names it too, so the log half of the
         * check cannot pass vacuously.
         */
        void assertRefusedOn(String constraint) {
            assertThat(Refusals.constraintOf(refused)).as("constraint named by the refusal").isEqualTo(constraint);
            assertThat(records).anyMatch(record -> LogCapture.text(record).contains(constraint));
        }

        /**
         * The token hash is in no record or message, in hex or in PostgreSQL's {@code \x} form, whole or as a prefix:
         * the server cuts a long field value in a "Failing row contains" line, so a refused row would show only the
         * start of the hash.
         */
        void assertNoTokenHash(byte[] tokenHash) {
            String hex = HexFormat.of().formatHex(tokenHash);
            assertNoneContains(hex);
            assertNoneContains("\\x" + hex);
            assertNoneContains(hex.substring(0, 16));
        }

        void assertNoneContains(String value) {
            for (LogRecord record : records) {
                assertThat(LogCapture.text(record)).as("log record %s", record.getLoggerName()).doesNotContain(value);
            }
            Refusals.assertNoValueIn(refused, value);
        }
    }
}
