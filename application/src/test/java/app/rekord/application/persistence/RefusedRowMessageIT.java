package app.rekord.application.persistence;

import static app.rekord.application.persistence.Refusals.assertNoValueIn;
import static app.rekord.application.persistence.Refusals.constraintOf;
import static app.rekord.application.persistence.Refusals.psqlOf;
import static app.rekord.application.persistence.Refusals.refusedBy;
import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/**
 * stop-crypto-logging, BR-ID-09: PostgreSQL puts the refused values into the error detail ({@code Key
 * (lower(email))=(…) already exists}, {@code Failing row contains (…)}), and pgjdbc copies it into the exception
 * message, which Hibernate logs. {@code logServerErrorDetail=false} keeps the values out; the constraint name and the
 * SQLState stay. Each case runs in one transaction on a temp table that is rolled back, so it needs no business table.
 */
@QuarkusTest
class RefusedRowMessageIT {

    private static final String ADDRESS = "planner@example.com";
    private static final String OTHER_CASE = "Planner@example.com";
    private static final String PASSWORD_HASH = "$argon2id$made-up-hash";

    @Inject
    DataSource dataSource;

    @Test
    void a_unique_violation_names_its_constraint_but_not_the_duplicate_value() throws SQLException {
        // Given a temp table with a unique index over the lower-cased address and one accepted row
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("create temp table refused_user (email text not null) on commit drop");
                s.execute("create unique index ux_refused_user_email on refused_user (lower(email))");
                insert(c, "refused_user", ADDRESS);

                // When the same address in other case is inserted
                Throwable refusal = refusedBy(() -> insert(c, "refused_user", OTHER_CASE));

                // Then it is refused on its constraint, and no message of the chain shows the address
                assertThat(psqlOf(refusal).getSQLState()).isEqualTo("23505");
                assertThat(constraintOf(refusal)).isEqualTo("ux_refused_user_email");
                assertNoValueIn(refusal, "example.com");
            } finally {
                c.rollback();
            }
        }
    }

    @Test
    void a_check_violation_names_its_constraint_but_not_the_failing_row() throws SQLException {
        // Given a temp table with a status check
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("""
                        create temp table refused_account (
                            email text not null,
                            password_hash text not null,
                            status text not null,
                            constraint refused_account_status_check check (status in ('INVITED', 'ACTIVE'))
                        ) on commit drop""");

                // When a row with a bad status is inserted
                Throwable refusal = refusedBy(() -> {
                    try (PreparedStatement p = c.prepareStatement(
                            "insert into refused_account (email, password_hash, status) values (?, ?, ?)")) {
                        p.setString(1, ADDRESS);
                        p.setString(2, PASSWORD_HASH);
                        p.setString(3, "NOPE");
                        p.executeUpdate();
                    }
                });

                // Then the check is named, and neither the hash nor the address is in any message
                assertThat(psqlOf(refusal).getSQLState()).isEqualTo("23514");
                assertThat(constraintOf(refusal)).isEqualTo("refused_account_status_check");
                assertNoValueIn(refusal, PASSWORD_HASH);
                assertNoValueIn(refusal, "example.com");
            } finally {
                c.rollback();
            }
        }
    }

    @Test
    void a_batched_insert_that_fails_does_not_echo_its_parameters() throws SQLException {
        // Given the unique temp table again
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("create temp table refused_batch (email text not null) on commit drop");
                s.execute("create unique index ux_refused_batch_email on refused_batch (lower(email))");

                // When the duplicate goes through addBatch / executeBatch
                Throwable refusal = refusedBy(() -> {
                    try (PreparedStatement p = c.prepareStatement("insert into refused_batch (email) values (?)")) {
                        p.setString(1, ADDRESS);
                        p.addBatch();
                        p.setString(1, OTHER_CASE);
                        p.addBatch();
                        p.executeBatch();
                    }
                });

                // Then the batch exception and its next exception do not contain the address
                assertThat(refusal).isInstanceOf(BatchUpdateException.class);
                assertThat((Object) ((BatchUpdateException) refusal).getNextException()).isNotNull();
                assertNoValueIn(refusal, "example.com");
            } finally {
                c.rollback();
            }
        }
    }

    private static void insert(Connection c, String table, String email) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("insert into " + table + " (email) values (?)")) {
            p.setString(1, email);
            p.executeUpdate();
        }
    }
}
