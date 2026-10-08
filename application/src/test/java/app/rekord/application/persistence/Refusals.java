package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.postgresql.util.PSQLException;

/**
 * The checks of a refused statement, shared by the repository and logging tests: catch what the statement threw, find
 * the PSQLException in the cause chain, read the constraint it names, and prove that a value shows nowhere in the
 * exception graph. Test data is made up (example.com addresses, fake hashes), so a failure may echo it.
 */
public final class Refusals {

    private Refusals() {}

    /** Runs the action, which must throw, and returns what it threw. */
    public static Throwable refusedBy(ThrowingCallable action) {
        Throwable thrown = catchThrowable(action);
        if (thrown == null) {
            throw new AssertionError("the action was expected to be refused, and was accepted");
        }
        return thrown;
    }

    /** The first PSQLException of the cause chain of the refusal. */
    public static PSQLException psqlOf(Throwable refusal) {
        for (Throwable t = refusal; t != null; t = t.getCause()) {
            if (t instanceof PSQLException psql) {
                return psql;
            }
        }
        throw new AssertionError("no PSQLException in the cause chain", refusal);
    }

    /**
     * The name of the constraint or index that PostgreSQL named in the refusal, or null when the server named none.
     * It stays readable with {@code logServerErrorDetail=false}, which suppresses only the values.
     */
    public static String constraintOf(Throwable refusal) {
        var detail = psqlOf(refusal).getServerErrorMessage();
        return detail == null ? null : detail.getConstraint();
    }

    /**
     * Fails when the value shows in the message or the {@code toString} of the refusal, of any cause, of any next
     * exception ({@code SQLException#getNextException}), or of any suppressed exception, at any depth.
     */
    public static void assertNoValueIn(Throwable refusal, String value) {
        assertNoValueIn(refusal, value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void assertNoValueIn(Throwable t, String value, Set<Throwable> seen) {
        if (t == null || !seen.add(t)) {
            return;
        }
        assertThat(String.valueOf(t.getMessage())).as("message of %s", t.getClass().getName()).doesNotContain(value);
        assertThat(t.toString()).as("toString of %s", t.getClass().getName()).doesNotContain(value);
        assertNoValueIn(t.getCause(), value, seen);
        for (Throwable suppressed : t.getSuppressed()) {
            assertNoValueIn(suppressed, value, seen);
        }
        if (t instanceof SQLException sql) {
            assertNoValueIn(sql.getNextException(), value, seen);
        }
    }
}
