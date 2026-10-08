package app.rekord.application.error;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Keeps a cause cycle from reaching Quarkus REST (TASK-5.7). After the exception mapper has answered, Quarkus REST
 * 3.39.1 walks the exception's {@code getCause()} chain with no visited set
 * ({@code RuntimeExceptionMapper.isKnownProblem}), so a chain that loops back keeps a worker thread busy for good and
 * the response is never written. When a resource method throws such a chain, this rethrows a {@link RedactedCause}
 * copy, which has none; every other exception, and every normal return, passes through untouched.
 *
 * <p>It is outermost ({@code PLATFORM_BEFORE}, ahead of the framework's own interceptors) so it sees what validation,
 * security and transactions throw too. The catch-all mapper then answers the 500 {@code UNKNOWN} envelope and logs its
 * one "Unhandled exception" line; {@code RedactedCause.of} returns the copy as it is, so that line is unchanged. The
 * price: a {@code RekordException} or {@code WebApplicationException} whose chain loops back answers that 500 and not
 * its own status, because the copy is neither (owner-approved, 2026-10-08).
 *
 * <p>Bound to the resource classes by {@link BindAcyclicCauseToResources}, not by hand.
 */
@AcyclicCause
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AcyclicCauseInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } catch (Throwable failure) {
            if (hasCauseCycle(failure)) {
                throw RedactedCause.of(failure);
            }
            throw failure;
        }
    }

    /** Whether following {@code getCause()} from {@code top} reaches a throwable it has already passed. */
    static boolean hasCauseCycle(Throwable top) {
        Set<Throwable> passed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable link = top; link != null; link = link.getCause()) {
            if (!passed.add(link)) {
                return true;
            }
        }
        return false;
    }
}
