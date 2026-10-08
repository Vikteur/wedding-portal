package app.rekord.application.error;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Test-only (TASK-5.7): throws a cyclic cause chain the way another interceptor of the application could (validation,
 * a transaction). It runs at {@code LIBRARY_BEFORE}, after the cause-cycle guard's {@code PLATFORM_BEFORE}, so the
 * guard sees the chain only while it is still the outermost interceptor: a guard bound any later would let it out.
 */
@ThrowsCyclicChain
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class CyclicChainInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) {
        throw ErrorEnvelopeProbeResource.chain(true);
    }
}
