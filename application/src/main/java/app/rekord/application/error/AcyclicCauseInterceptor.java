package app.rekord.application.error;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Not built yet (TASK-5.7): the tests that pin its behaviour fail first. */
@AcyclicCause
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AcyclicCauseInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        throw new UnsupportedOperationException("TASK-5.7: the guard is not built yet");
    }
}
