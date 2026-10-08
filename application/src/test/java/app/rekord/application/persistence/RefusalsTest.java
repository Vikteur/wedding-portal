package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

class RefusalsTest {

    private static final String VALUE = "planner@example.com";

    @Test
    void a_refused_action_returns_what_it_threw() {
        // Given an action that throws
        IllegalStateException thrown = new IllegalStateException("no");

        // Then refusedBy returns that throwable
        assertThat(Refusals.refusedBy(() -> {
                    throw thrown;
                }))
                .isSameAs(thrown);
    }

    @Test
    void an_accepted_action_is_an_assertion_error() {
        // Given an action that completes
        // Then refusedBy fails: the test expected a refusal
        assertThatThrownBy(() -> Refusals.refusedBy(() -> {}))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected to be refused");
    }

    @Test
    void the_psql_exception_is_found_below_wrappers_and_its_constraint_is_read() {
        // Given a PSQLException that names a constraint, wrapped twice
        PSQLException psql = new PSQLException(new ServerErrorMessage("SERROR\0C23505\0Mduplicate\0nux_users_email\0"));
        Throwable refusal = new RuntimeException("outer", new IllegalStateException("middle", psql));

        // Then it is found, with its SQLState and constraint name
        assertThat((Object) Refusals.psqlOf(refusal)).isSameAs(psql);
        assertThat(Refusals.psqlOf(refusal).getSQLState()).isEqualTo("23505");
        assertThat(Refusals.constraintOf(refusal)).isEqualTo("ux_users_email");
    }

    @Test
    void a_chain_without_a_psql_exception_is_an_assertion_error_that_keeps_the_chain() {
        // Given a chain with no PSQLException
        Throwable refusal = new RuntimeException("outer", new IllegalStateException("inner"));

        // Then psqlOf fails, and the failure carries the chain as its cause
        assertThatThrownBy(() -> Refusals.psqlOf(refusal))
                .isInstanceOf(AssertionError.class)
                .hasCause(refusal);
    }

    @Test
    void a_psql_exception_without_a_server_message_has_no_constraint() {
        // Given a driver-side PSQLException (no server error message)
        Throwable refusal = new PSQLException("closed", PSQLState.CONNECTION_FAILURE);

        // Then there is no constraint to read
        assertThat(Refusals.constraintOf(refusal)).isNull();
    }

    @Test
    void a_chain_that_holds_the_value_nowhere_passes() {
        // Given a chain, a next exception and a suppressed exception that do not hold the value
        SQLException sql = new SQLException("refused");
        sql.setNextException(new SQLException("next"));
        RuntimeException refusal = new RuntimeException("outer", sql);
        refusal.addSuppressed(new IllegalStateException("close failed"));

        // Then the check passes
        Refusals.assertNoValueIn(refusal, VALUE);
    }

    @Test
    void the_value_is_found_in_the_message_of_the_throwable_itself() {
        assertFound(new RuntimeException("duplicate " + VALUE));
    }

    @Test
    void the_value_is_found_in_a_cause() {
        assertFound(new RuntimeException("outer", new IllegalStateException("inner " + VALUE)));
    }

    @Test
    void the_value_is_found_in_a_to_string_that_the_message_does_not_show() {
        // Given an exception whose message is clean but whose toString adds a field
        Throwable refusal = new RuntimeException("clean") {
            @Override
            public String toString() {
                return "refused for " + VALUE;
            }
        };

        // Then the value is found
        assertFound(refusal);
    }

    @Test
    void the_value_is_found_in_a_next_exception_and_in_the_cause_of_a_next_exception() {
        // Given a refusal with a next exception that holds the value
        SQLException withNext = new SQLException("refused");
        withNext.setNextException(new SQLException("detail " + VALUE));
        assertFound(withNext);

        // And a refusal whose next exception hides it one cause further down
        SQLException withDeepNext = new SQLException("refused");
        withDeepNext.setNextException(new SQLException("next", new IllegalStateException("cause " + VALUE)));
        assertFound(withDeepNext);

        // And one whose cause carries the next exception
        SQLException inner = new SQLException("inner");
        inner.setNextException(new SQLException("detail " + VALUE));
        assertFound(new RuntimeException("outer", inner));
    }

    @Test
    void the_value_is_found_in_a_suppressed_exception_and_below_it() {
        // Given a refusal with a suppressed exception that holds the value
        RuntimeException refusal = new RuntimeException("outer");
        refusal.addSuppressed(new IllegalStateException("rollback failed for " + VALUE));
        assertFound(refusal);

        // And one whose suppressed exception hides it in its own cause
        RuntimeException deep = new RuntimeException("outer");
        deep.addSuppressed(new IllegalStateException("close", new IllegalArgumentException(VALUE)));
        assertFound(deep);
    }

    @Test
    void a_chain_that_loops_back_to_itself_is_walked_once() {
        // Given two exceptions that are each other's suppressed exception
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b");
        a.addSuppressed(b);
        b.addSuppressed(a);

        // Then the check ends, and passes
        Refusals.assertNoValueIn(a, VALUE);
    }

    private static void assertFound(Throwable refusal) {
        assertThatThrownBy(() -> Refusals.assertNoValueIn(refusal, VALUE)).isInstanceOf(AssertionError.class);
    }
}
