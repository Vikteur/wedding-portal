package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * TASK-24.8: a golden fixture that is missing or not valid JSON is reported by name on every access of the gate,
 * not once as an {@code ExceptionInInitializerError} and then as a bare {@code NoClassDefFoundError}.
 *
 * <p>The checked-in fixtures are never touched: each gate test runs {@code GoldenGate} in a class loader of its own
 * that hides one fixture, or serves {@code golden-gate-test/not-json.json} in its place.
 */
class GoldenGateFixtureLoadingTest {

    private static final String SET = "/golden/golden-set.json";
    private static final String LIBRARY = "/golden/golden-library.json";
    private static final String NOT_JSON = "/golden-gate-test/not-json.json";

    // The fixture, on its own (what GoldenGate holds for each of the two files)

    @Test
    void a_fixture_missing_from_the_classpath_is_named_by_every_read() {
        GoldenGate.Fixture missing = new GoldenGate.Fixture("/golden/not-there.json");

        for (int call = 1; call <= 2; call++) {
            assertThatThrownBy(missing::get).as("read %d", call)
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("/golden/not-there.json");
        }
    }

    @Test
    void a_fixture_that_is_not_json_is_reported_with_its_name_and_the_parse_error_on_every_read() {
        GoldenGate.Fixture notJson = new GoldenGate.Fixture(NOT_JSON);

        for (int call = 1; call <= 2; call++) {
            assertThatThrownBy(notJson::get).as("read %d", call)
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(NOT_JSON)
                    .hasCauseInstanceOf(JsonProcessingException.class)
                    .satisfies(e -> assertThat(e.getMessage())
                            .contains(((JsonProcessingException) e.getCause()).getOriginalMessage()));
        }
    }

    @Test
    void a_fixture_that_reads_is_parsed_once() {
        GoldenGate.Fixture set = new GoldenGate.Fixture(SET);

        assertThat(set.get()).isNotNull().isSameAs(set.get());
    }

    // The gate, from its own access paths, with a fixture hidden or replaced

    @Test
    void the_isolated_gate_reads_the_checked_in_fixtures() throws ReflectiveOperationException {
        // Given the isolated gate with nothing hidden (so a failure below is the fixture's, not the harness's)
        IsolatedGate gate = new IsolatedGate(null, null);

        // When it hands out the golden set
        JsonNode set = (JsonNode) gate.access("set");

        // Then it holds the 212 cases
        assertThat(set.get("cases")).hasSize(212);
    }

    @ParameterizedTest
    @ValueSource(strings = {"set", "answers"})
    void a_missing_golden_set_is_named_by_every_access(String access) throws ReflectiveOperationException {
        IsolatedGate gate = new IsolatedGate(SET, null);

        for (int call = 1; call <= 2; call++) {
            assertThat(failureOf(gate, access)).as("%s(), call %d", access, call)
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(SET);
        }
    }

    @Test
    void a_missing_golden_library_is_named_by_every_access() throws ReflectiveOperationException {
        IsolatedGate gate = new IsolatedGate(LIBRARY, null);

        for (int call = 1; call <= 2; call++) {
            assertThat(failureOf(gate, "answers")).as("answers(), call %d", call)
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(LIBRARY);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"set", "answers"})
    void a_golden_set_that_is_not_json_is_reported_with_its_name_and_the_parse_error_by_every_access(String access)
            throws ReflectiveOperationException {
        IsolatedGate gate = new IsolatedGate(SET, NOT_JSON);

        for (int call = 1; call <= 2; call++) {
            assertParseFailure(failureOf(gate, access), SET, access + "(), call " + call);
        }
    }

    @Test
    void a_golden_library_that_is_not_json_is_reported_with_its_name_and_the_parse_error_by_every_access()
            throws ReflectiveOperationException {
        IsolatedGate gate = new IsolatedGate(LIBRARY, NOT_JSON);

        for (int call = 1; call <= 2; call++) {
            assertParseFailure(failureOf(gate, "answers"), LIBRARY, "answers(), call " + call);
        }
    }

    private static void assertParseFailure(Throwable failure, String resource, String where) {
        assertThat(failure).as(where)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining(resource)
                .hasCauseInstanceOf(JsonProcessingException.class);
        assertThat(failure.getMessage()).as(where)
                .contains(((JsonProcessingException) failure.getCause()).getOriginalMessage());
    }

    /** What the access throws; an initialisation error is returned too, so the assertion names it. */
    private static Throwable failureOf(IsolatedGate gate, String access) throws ReflectiveOperationException {
        try {
            gate.access(access);
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (LinkageError e) {
            return e;
        }
        throw new AssertionError(access + "() was expected to fail");
    }

    /**
     * Defines the {@code app.rekord.domain} classes itself, so {@code GoldenGate} initialises afresh, and answers
     * the lookup of one golden resource with nothing ({@code by == null}) or with another classpath resource.
     */
    private static final class IsolatedGate extends ClassLoader {

        private static final String GATE = "app.rekord.domain.matching.GoldenGate";

        private final String replaced;
        private final String by;

        IsolatedGate(String replaced, String by) {
            super(GoldenGateFixtureLoadingTest.class.getClassLoader());
            this.replaced = replaced == null ? null : replaced.substring(1);
            this.by = by == null ? null : by.substring(1);
        }

        /** Calls a static, no-argument method of {@code GoldenGate} in this loader. */
        Object access(String method) throws ReflectiveOperationException {
            Method m = loadClass(GATE).getDeclaredMethod(method);
            m.setAccessible(true);
            return m.invoke(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("app.rekord.domain.")) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (in == null) {
                            throw new ClassNotFoundException(name);
                        }
                        byte[] bytes = in.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        @Override
        public URL getResource(String name) {
            if (name.equals(replaced)) {
                return by == null ? null : getParent().getResource(by);
            }
            return super.getResource(name);
        }
    }
}
