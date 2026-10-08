package app.rekord.application.error;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds {@link AcyclicCauseInterceptor} to a resource class (TASK-5.7). Nothing writes it by hand:
 * {@link BindAcyclicCauseToResources} adds it at build time.
 */
@InterceptorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AcyclicCause {}
