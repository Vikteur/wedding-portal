package app.rekord.application.error;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A copy of a throwable chain that keeps only what is code: each class name and its stack frames. Every message is
 * dropped, because a message can carry SQL, a row value, an e-mail address or a token (UD-19.f).
 */
final class RedactedCause extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String className;
    private RedactedCause redactedCause;

    private RedactedCause(String className) {
        super(null, null, true, true);
        this.className = className;
    }

    static RedactedCause of(Throwable original) {
        return copy(original, new IdentityHashMap<>());
    }

    /** Each throwable is copied once; a link back to one already copied (a cycle) is cut, so no renderer can loop. */
    private static RedactedCause copy(Throwable original, Map<Throwable, RedactedCause> done) {
        RedactedCause copy = new RedactedCause(original.getClass().getName());
        done.put(original, copy);
        copy.setStackTrace(original.getStackTrace());
        Throwable cause = original.getCause();
        if (cause != null && !done.containsKey(cause)) {
            copy.redactedCause = copy(cause, done);
        }
        for (Throwable suppressed : original.getSuppressed()) {
            if (!done.containsKey(suppressed)) {
                copy.addSuppressed(copy(suppressed, done));
            }
        }
        return copy;
    }

    /** Overridden because the super constructor has already fixed the cause as absent, which initCause cannot undo. */
    @Override
    public synchronized Throwable getCause() {
        return redactedCause;
    }

    @Override
    public String toString() {
        return className;
    }
}
