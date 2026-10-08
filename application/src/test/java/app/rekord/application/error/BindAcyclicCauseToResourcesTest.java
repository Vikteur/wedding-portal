package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The build-time binding of the cause-cycle guard (TASK-5.7) must never skip a class silently: when it cannot read a
 * class's super types the build fails, and the failure has to say which class it was.
 */
class BindAcyclicCauseToResourcesTest {

    private final BindAcyclicCauseToResources extension = new BindAcyclicCauseToResources();

    /** A class model that answers by method name, the way the CDI Lite model of ArC answers about a real class. */
    private static ClassInfo classInfo(Map<String, Function<Object[], Object>> answers) {
        InvocationHandler handler = (proxy, method, args) -> {
            Function<Object[], Object> answer = answers.get(method.getName());
            if (answer == null) {
                throw new UnsupportedOperationException(method.getName());
            }
            return answer.apply(args);
        };
        return (ClassInfo) Proxy.newProxyInstance(
                BindAcyclicCauseToResourcesTest.class.getClassLoader(), new Class<?>[] {ClassInfo.class}, handler);
    }

    /** A concrete class that is not a resource and has no other interface, with the given super class. */
    private static ClassInfo concreteClass(String name, ClassInfo superClass) {
        return classInfo(Map.of(
                "name", args -> name,
                "isPlainClass", args -> true,
                "isAbstract", args -> false,
                "hasAnnotation", args -> false,
                "superInterfacesDeclarations", args -> List.of(),
                "superClassDeclaration", args -> superClass));
    }

    /** What ArC 3.39.1 hands over for a super type that is not on the build classpath: a model that cannot be read. */
    private static ClassInfo unreadableSuperClass() {
        return classInfo(Map.of("name", args -> {
            throw new NullPointerException("the class behind this model is missing from the index");
        }));
    }

    /** A class config that records the annotations added to it. */
    private static ClassConfig classConfig(ClassInfo info, List<Class<?>> added) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "info":
                    return info;
                case "addAnnotation":
                    added.add((Class<?>) args[0]);
                    return proxy;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        };
        return (ClassConfig) Proxy.newProxyInstance(
                BindAcyclicCauseToResourcesTest.class.getClassLoader(), new Class<?>[] {ClassConfig.class}, handler);
    }

    @Test
    void a_class_whose_super_type_cannot_be_read_fails_the_build_naming_that_class() {
        // Given: ArC wraps a super type missing from the build classpath instead of answering null
        List<Class<?>> added = new ArrayList<>();
        ClassConfig candidate =
                classConfig(concreteClass("app.example.WeddingThing", unreadableSuperClass()), added);

        // When / Then: loud, never skipped, and the message says which class and which ticket's extension
        assertThatThrownBy(() -> extension.bind(candidate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.example.WeddingThing")
                .hasMessageContaining("TASK-5.7")
                .hasCauseInstanceOf(NullPointerException.class);
        assertThat(added).isEmpty();
    }

    @Test
    void a_class_with_a_readable_hierarchy_and_no_path_is_left_alone() {
        // Given
        List<Class<?>> added = new ArrayList<>();
        ClassConfig candidate = classConfig(concreteClass("app.example.Plain", null), added);

        // When
        extension.bind(candidate);

        // Then
        assertThat(added).isEmpty();
    }
}
