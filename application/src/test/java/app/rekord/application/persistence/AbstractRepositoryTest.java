package app.rekord.application.persistence;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;

/**
 * The base of every repository test (architecture-conventions section 8.5): an injected {@code EntityManager}, a new
 * transaction per action, a refusal read by SQLState and constraint name, and every business table emptied after each
 * test. Subclasses carry {@code @QuarkusTest}.
 */
public abstract class AbstractRepositoryTest {

    /** What PostgreSQL refused: the SQLState and the name of the constraint or index. */
    public record Refusal(String sqlState, String constraint) {}

    @Inject
    protected EntityManager em;

    @Inject
    protected DataSource dataSource;

    @AfterEach
    void emptyTheTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("""
                    select table_name from information_schema.tables
                    where table_schema = 'public' and table_type = 'BASE TABLE'
                      and table_name <> 'flyway_schema_history'""")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            if (!tables.isEmpty()) {
                s.execute("truncate table " + String.join(", ", tables) + " cascade");
            }
        }
    }

    protected void inNewTransaction(Runnable action) {
        QuarkusTransaction.requiringNew().run(action);
    }

    protected <T> T inNewTransactionReturning(Supplier<T> action) {
        return QuarkusTransaction.requiringNew().call(action::get);
    }

    /** Runs the action in a new transaction, expects it to throw, and returns what it threw. */
    protected Throwable refusedWith(Runnable action) {
        return Refusals.refusedBy(() -> QuarkusTransaction.requiringNew().run(action));
    }

    /** Like {@link #refusedWith}, reading SQLState and constraint name from the PSQLException in the cause chain. */
    protected Refusal refusal(Runnable action) {
        Throwable thrown = refusedWith(action);
        return new Refusal(Refusals.psqlOf(thrown).getSQLState(), Refusals.constraintOf(thrown));
    }
}
