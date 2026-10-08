package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.application.ResourceTestProfile;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.Subclass;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.ws.rs.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The cause-cycle guard has to sit on every resource method (TASK-5.7). Nobody writes the binding on a resource, so a
 * resource added later is covered only if the build step that adds it keeps finding resources: this fails when one is
 * left out, which the cyclic probes alone cannot show for a resource that has no probe.
 */
@QuarkusTest
@TestProfile(ResourceTestProfile.class)
@Tag("resource-test")
class AcyclicCauseBindingIT {

    private static boolean isResource(Class<?> type) {
        if (type == null || type == Object.class) {
            return false;
        }
        if (type.isAnnotationPresent(Path.class)) {
            return true;
        }
        for (Class<?> implemented : type.getInterfaces()) {
            if (isResource(implemented)) {
                return true;
            }
        }
        return isResource(type.getSuperclass());
    }

    @Test
    void the_interceptor_is_the_one_bean_that_answers_the_binding() {
        // Given
        BeanManager beans = Arc.container().beanManager();

        // When
        List<Interceptor<?>> answering =
                beans.resolveInterceptors(InterceptionType.AROUND_INVOKE, new AnnotationLiteral<AcyclicCause>() {});

        // Then
        assertThat(answering).extracting(Interceptor::getBeanClass).containsExactly(AcyclicCauseInterceptor.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void every_jaxrs_resource_bean_is_intercepted_whether_the_path_is_on_its_class_or_on_an_interface() {
        // Given: every bean whose class, a superclass or an interface carries @Path
        List<Bean<?>> resources = Arc.container().beanManager().getBeans(Object.class, Any.Literal.INSTANCE).stream()
                .filter(bean -> isResource(bean.getBeanClass()))
                .toList();

        // Then: the three kinds of resource in this application are among them
        assertThat(resources).extracting(bean -> bean.getBeanClass().getSimpleName())
                .contains(
                        "HealthResource", // the real one: the @Path is on the generated HealthApi
                        "InterfaceProbeResource", // the same shape, with a probe that throws
                        "ErrorEnvelopeProbeResource", // the @Path is on the class
                        "SuccessStatusProbeResource");

        // And: each is served through a generated subclass, which is how ArC applies an interceptor to a class bean
        for (Bean<?> resource : resources) {
            Object served = Arc.container().instance((InjectableBean<Object>) resource).get();
            Object contextual = served instanceof ClientProxy proxy ? proxy.arc_contextualInstance() : served;
            assertThat(contextual)
                    .as("%s is served through an interceptor subclass", resource.getBeanClass().getName())
                    .isInstanceOf(Subclass.class);
        }
    }
}
