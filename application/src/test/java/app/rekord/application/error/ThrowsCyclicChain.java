package app.rekord.application.error;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test-only (TASK-5.7): marks a method whose call is answered by {@link CyclicChainInterceptor}, which throws a
 * cyclic cause chain before the method body runs.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ThrowsCyclicChain {}
