package app.rekord.usecase.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.usecase.probe.port.ProbeRowPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbeRowUseCaseTest {

    @Test
    void save_inserts_first_and_confirms_second() {
        // Given a recording port
        RecordingPort port = new RecordingPort();

        // When a marker is saved
        new ProbeRowUseCase(port).save("m");

        // Then the port saw insert, then confirm
        assertThat(port.calls).containsExactly("insert(m)", "confirm(m)");
    }

    @Test
    void a_failing_confirm_reaches_the_caller_unwrapped() {
        // Given a port whose confirm fails
        RecordingPort port = new RecordingPort();
        port.failure = new IllegalStateException("confirm failed");

        // When / Then the caller gets that very exception
        assertThatThrownBy(() -> new ProbeRowUseCase(port).save("m")).isSameAs(port.failure);
        assertThat(port.calls).containsExactly("insert(m)", "confirm(m)");
    }

    @Test
    void the_use_case_is_an_application_scoped_class_with_one_constructor_and_a_transactional_save() throws Exception {
        // Given the use case class
        Class<?> type = ProbeRowUseCase.class;

        // When / Then
        assertThat(type.isAnnotationPresent(ApplicationScoped.class)).isTrue();
        assertThat(type.getDeclaredConstructors()).hasSize(1);
        assertThat(type.getDeclaredConstructors()[0].getParameterTypes()).containsExactly(ProbeRowPort.class);
        assertThat(type.getMethod("save", String.class).isAnnotationPresent(Transactional.class)).isTrue();
    }

    private static final class RecordingPort implements ProbeRowPort {
        final List<String> calls = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void insert(String marker) {
            calls.add("insert(" + marker + ")");
        }

        @Override
        public void confirm(String marker) {
            calls.add("confirm(" + marker + ")");
            if (failure != null) {
                throw failure;
            }
        }
    }
}
