package app.rekord.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DomainRulesTest {

    private static void assertBreaksA1(JavaClasses fixtures, String fixtureClass) {
        assertThatThrownBy(() -> DomainRules.A1.check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("A1")
                .hasMessageContaining(fixtureClass);
    }

    @Test
    void a_domain_class_importing_a_jakarta_type_breaks_a1() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                import jakarta.inject.Inject;

                public class InjectedThing {
                    @Inject
                    String name;
                }
                """);

        assertBreaksA1(fixtures, "app.rekord.domain.fixture.InjectedThing");
    }

    @Test
    void a_domain_class_importing_an_io_quarkus_type_breaks_a1() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                import io.quarkus.runtime.StartupEvent;

                public class StartupAware {
                    StartupEvent event;
                }
                """);

        assertBreaksA1(fixtures, "app.rekord.domain.fixture.StartupAware");
    }

    @ParameterizedTest
    @CsvSource({
        "org.hibernate.Session",
        "com.fasterxml.jackson.databind.ObjectMapper",
        "io.smallrye.config.SmallRyeConfig",
        "org.eclipse.microprofile.config.Config"
    })
    void a_domain_class_depending_on_a_framework_package_breaks_a1(String type) {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                public class FrameworkUser {
                    %s field;
                }
                """
                        .formatted(type));

        assertBreaksA1(fixtures, "app.rekord.domain.fixture.FrameworkUser");
    }

    @Test
    void a_domain_class_depending_on_a_usecase_class_breaks_a1() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.usecase.fixture;

                public class SomeUseCase {}
                """,
                """
                package app.rekord.domain.fixture;

                import app.rekord.usecase.fixture.SomeUseCase;

                public class UpwardLooker {
                    SomeUseCase useCase;
                }
                """);

        assertBreaksA1(fixtures, "app.rekord.domain.fixture.UpwardLooker");
    }

    @Test
    void a_domain_class_using_only_java_and_domain_types_passes_a1() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                public record Money(long cents) {}
                """,
                """
                package app.rekord.domain.fixture;

                import java.time.Instant;
                import java.util.List;

                public record Booking(Money price, Instant at, List<String> notes) {
                    Money doubled() {
                        return new Money(price.cents() * 2);
                    }
                }
                """);

        assertThatCode(() -> DomainRules.A1.check(fixtures)).doesNotThrowAnyException();
    }

    @Test
    void the_suite_checks_a1() {
        assertThat(Arrays.stream(DomainArchitectureTest.class.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(ArchTest.class))
                        .map(DomainRulesTest::rule)
                        .map(ArchRule::getDescription))
                .anyMatch(description -> description.contains("A1"));
    }

    private static ArchRule rule(Field field) {
        try {
            field.setAccessible(true);
            return (ArchRule) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
