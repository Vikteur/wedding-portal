package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class HealthResourceImplementsContractTest {

    private static final String ADAPTER_RESOURCE = "app.rekord.adapter.web.health.HealthResource";

    private static boolean isJaxRs(Annotation annotation) {
        return annotation.annotationType().getName().startsWith("jakarta.ws.rs");
    }

    @Test
    void the_adapter_health_resource_implements_the_generated_health_api() throws Exception {
        // Given the adapter's resource
        Class<?> resource = Class.forName(ADAPTER_RESOURCE);

        // Then it implements the generated interface
        assertThat(Class.forName("app.rekord.api.HealthApi")).isAssignableFrom(resource);
    }

    @Test
    void paths_and_verbs_come_from_the_interface_not_from_the_resource() throws Exception {
        // Given the adapter's resource
        Class<?> resource = Class.forName(ADAPTER_RESOURCE);

        // Then neither the class nor its methods carry a jakarta.ws.rs annotation
        assertThat(Arrays.stream(resource.getDeclaredAnnotations()).filter(HealthResourceImplementsContractTest::isJaxRs))
                .isEmpty();
        for (Method method : resource.getDeclaredMethods()) {
            assertThat(Stream.concat(
                                    Arrays.stream(method.getDeclaredAnnotations()),
                                    Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream))
                            .filter(HealthResourceImplementsContractTest::isJaxRs))
                    .as(method.getName())
                    .isEmpty();
        }
    }

    @Test
    void the_hand_written_health_resource_and_record_are_gone() {
        assertThatThrownBy(() -> Class.forName("app.rekord.application.health.HealthResource"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("app.rekord.application.health.Health"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
