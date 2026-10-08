package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.interceptor.InvocationContext;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

/**
 * The guard against Quarkus REST looping on a cyclic cause chain (TASK-5.7): it must leave every exception alone but
 * one whose cause chain loops back.
 */
class AcyclicCauseInterceptorTest {

    private final AcyclicCauseInterceptor interceptor = new AcyclicCauseInterceptor();

    /** An invocation context whose {@code proceed} runs the body; the interceptor needs nothing else from it. */
    private static InvocationContext invocation(Callable<Object> body) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("proceed")) {
                return body.call();
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (InvocationContext) Proxy.newProxyInstance(
                AcyclicCauseInterceptorTest.class.getClassLoader(), new Class<?>[] {InvocationContext.class}, handler);
    }

    /** What the interceptor throws when the resource method throws {@code failure}. */
    private Throwable thrownWhenMethodThrows(Exception failure) {
        return catchThrowable(() -> interceptor.around(invocation(() -> {
            throw failure;
        })));
    }

    /** The links of the cause chain, in order, stopping at the first link seen twice. */
    private static List<Throwable> causeLinks(Throwable top) {
        IdentityHashMap<Throwable, Boolean> seen = new IdentityHashMap<>();
        List<Throwable> links = new ArrayList<>();
        for (Throwable link = top; link != null && seen.put(link, true) == null; link = link.getCause()) {
            links.add(link);
        }
        return links;
    }

    @Test
    void a_normal_return_passes_through_as_it_is() throws Exception {
        Object result = new Object();

        assertThat(interceptor.around(invocation(() -> result))).isSameAs(result);
        assertThat(interceptor.around(invocation(() -> null))).isNull();
    }

    @Test
    void an_exception_without_a_cause_cycle_is_rethrown_as_the_same_instance() {
        // Given: sentinel messages, a cause, a suppressed exception: nothing here makes the framework loop
        IllegalStateException chain = ErrorEnvelopeProbeResource.chain(false);

        // When / Then: not copied, not redacted, so the mapper still sees the exception it would have seen
        assertThat(thrownWhenMethodThrows(chain)).isSameAs(chain);
    }

    @Test
    void a_checked_exception_and_one_without_a_cause_are_rethrown_as_they_are() {
        IOException checked = new IOException("member@example.com");
        IllegalArgumentException plain = new IllegalArgumentException("no cause");

        assertThat(thrownWhenMethodThrows(checked)).isSameAs(checked);
        assertThat(thrownWhenMethodThrows(plain)).isSameAs(plain);
    }

    @Test
    void the_same_cause_reached_twice_without_a_loop_is_not_a_cycle() {
        // Given: a shared cause hangs below two suppressed exceptions, which is not a loop of the cause chain
        IllegalStateException shared = new IllegalStateException("shared");
        IllegalStateException top = new IllegalStateException("top", new RuntimeException("middle", shared));
        top.addSuppressed(new IllegalArgumentException("first", shared));
        top.addSuppressed(new IllegalArgumentException("second", shared));

        assertThat(thrownWhenMethodThrows(top)).isSameAs(top);
    }

    @Test
    void a_long_cause_chain_without_a_loop_is_left_alone() {
        // Given
        RuntimeException top = new RuntimeException("bottom");
        for (int depth = 0; depth < 5_000; depth++) {
            top = new RuntimeException("level " + depth, top);
        }

        // When / Then
        assertThat(thrownWhenMethodThrows(top)).isSameAs(top);
    }

    @Test
    void a_cause_chain_that_loops_back_to_its_top_is_thrown_as_an_acyclic_redacted_copy() {
        // Given: the sentinel chain of the probe, whose SQLException has the top exception as its cause
        IllegalStateException cyclic = ErrorEnvelopeProbeResource.chain(true);

        // When
        Throwable thrown = thrownWhenMethodThrows(cyclic);

        // Then: a copy that keeps class names and frames, whose chain ends
        assertThat(thrown).isInstanceOf(RedactedCause.class).isNotSameAs(cyclic);
        assertThat(thrown.toString()).isEqualTo("java.lang.IllegalStateException");
        assertThat(thrown.getStackTrace()).isEqualTo(cyclic.getStackTrace());
        List<Throwable> links = causeLinks(thrown);
        assertThat(links).extracting(Throwable::toString)
                .containsExactly("java.lang.IllegalStateException", "java.sql.SQLException");
        assertThat(links.get(links.size() - 1).getCause()).isNull();
        assertThat(thrown.getMessage()).isNull();
        for (String sentinel : ErrorEnvelopeProbeResource.SENTINELS) {
            assertThat(thrown.toString()).doesNotContain(sentinel);
        }
    }

    @Test
    void a_loop_that_does_not_pass_through_the_top_exception_is_found_too() {
        // Given: top -> middle -> bottom -> middle
        RuntimeException bottom = new RuntimeException("bottom tok-example-123");
        RuntimeException middle = new RuntimeException("middle", bottom);
        bottom.initCause(middle);
        RuntimeException top = new RuntimeException("top", middle);

        // When
        Throwable thrown = thrownWhenMethodThrows(top);

        // Then
        assertThat(thrown).isInstanceOf(RedactedCause.class);
        List<Throwable> links = causeLinks(thrown);
        assertThat(links).hasSize(3);
        assertThat(links.get(2).getCause()).isNull();
    }

    @Test
    void an_exception_that_answers_itself_as_its_cause_is_a_loop() {
        // Given: Throwable.getCause hides a self-reference made by initCause, so a subclass has to answer it
        RuntimeException selfCaused = new RuntimeException("self") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        // When
        Throwable thrown = thrownWhenMethodThrows(selfCaused);

        // Then
        assertThat(thrown).isInstanceOf(RedactedCause.class);
        assertThat(thrown.getCause()).isNull();
    }
}
