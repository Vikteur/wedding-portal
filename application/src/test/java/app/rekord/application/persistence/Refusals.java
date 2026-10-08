package app.rekord.application.persistence;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.postgresql.util.PSQLException;

/** Not written yet: the checks of a refused statement, shared by the repository and logging tests (review L2). */
public final class Refusals {

    private Refusals() {}

    public static Throwable refusedBy(ThrowingCallable action) {
        return null;
    }

    public static PSQLException psqlOf(Throwable refusal) {
        return null;
    }

    public static String constraintOf(Throwable refusal) {
        return null;
    }

    public static void assertNoValueIn(Throwable refusal, String value) {}
}
