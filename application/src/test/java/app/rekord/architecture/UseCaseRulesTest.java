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

class UseCaseRulesTest {

    private static final String USE_CASE = "app.rekord.usecase.fixture.OffendingUseCase";

    private static void assertBreaksA3(JavaClasses fixtures) {
        assertThatThrownBy(() -> UseCaseRules.A3.check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("A3")
                .hasMessageContaining(USE_CASE);
    }

    private static JavaClasses useCaseWith(String members) {
        return FixtureCompiler.compile(
                "package app.rekord.usecase.fixture;\npublic class OffendingUseCase {\n" + members + "\n}\n");
    }

    @Test
    void a_use_case_annotated_application_scoped_with_a_transactional_method_passes_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.domain.fixture;

                public record Wedding(String name) {}
                """,
                """
                package app.rekord.usecase.fixture;

                import app.rekord.domain.fixture.Wedding;
                import jakarta.enterprise.context.ApplicationScoped;
                import jakarta.transaction.Transactional;
                import java.util.List;

                @ApplicationScoped
                public class CreateWedding {
                    @Transactional
                    public Wedding create(String name) {
                        return new Wedding(String.valueOf(List.of(name)));
                    }
                }
                """);

        assertThatCode(() -> UseCaseRules.A3.check(fixtures)).doesNotThrowAnyException();
    }

    @Test
    void another_jakarta_transaction_type_breaks_a3() {
        assertBreaksA3(useCaseWith("jakarta.transaction.TransactionManager manager;"));
    }

    @ParameterizedTest
    @CsvSource({"@jakarta.enterprise.context.RequestScoped", "@jakarta.inject.Inject"})
    void another_cdi_type_breaks_a3(String annotation) {
        assertBreaksA3(useCaseWith(annotation + " String field;"));
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.ws.rs.core.Response",
        "jakarta.persistence.EntityManager",
        "io.quarkus.runtime.StartupEvent",
        "io.vertx.core.Vertx",
        "com.fasterxml.jackson.databind.ObjectMapper"
    })
    void a_use_case_depending_on_a_framework_type_breaks_a3(String type) {
        assertBreaksA3(useCaseWith(type + " field;"));
    }

    @Test
    void a_use_case_depending_on_an_adapter_class_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.adapter.web.fixture;

                public class SomeResource {}
                """,
                """
                package app.rekord.usecase.fixture;

                public class OffendingUseCase {
                    app.rekord.adapter.web.fixture.SomeResource resource;
                }
                """);

        assertBreaksA3(fixtures);
    }

    @Test
    void the_a3_exception_is_written_into_the_rule() {
        assertThat(UseCaseRules.A3.getDescription())
                .contains("CT-11", "jakarta.transaction.Transactional", "jakarta.enterprise.context.ApplicationScoped");
    }

    private static final String CREATE_WEDDING =
            """
            package app.rekord.usecase.fixture;

            import jakarta.transaction.Transactional;

            public class CreateWedding {
                @Transactional
                public void create() {}
            }
            """;

    private static void assertBreaksA3On(JavaClasses fixtures, String offender) {
        assertThatThrownBy(() -> UseCaseRules.A3.check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("A3")
                .hasMessageContaining(offender);
    }

    @Test
    void a_producer_method_returning_a_use_case_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                CREATE_WEDDING,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.CreateWedding;
                import jakarta.enterprise.inject.Produces;

                public class UseCaseProducer {
                    @Produces
                    CreateWedding createWedding() {
                        return null;
                    }
                }
                """);

        assertBreaksA3On(fixtures, "app.rekord.application.fixture.UseCaseProducer");
    }

    @Test
    void a_producer_method_returning_a_use_case_through_its_port_interface_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.usecase.fixture.port;

                public interface CreateWeddingCommand {
                    void create();
                }
                """,
                """
                package app.rekord.usecase.fixture;

                import app.rekord.usecase.fixture.port.CreateWeddingCommand;

                public class CreateWedding implements CreateWeddingCommand {
                    public void create() {}
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.CreateWedding;
                import app.rekord.usecase.fixture.port.CreateWeddingCommand;
                import jakarta.enterprise.inject.Produces;

                public class UseCaseProducer {
                    @Produces
                    CreateWeddingCommand createWedding() {
                        return new CreateWedding();
                    }
                }
                """);

        assertBreaksA3On(fixtures, "app.rekord.application.fixture.UseCaseProducer");
    }

    private static final String CREATE_WEDDING_COMMAND =
            """
            package app.rekord.usecase.fixture.port;

            public interface CreateWeddingCommand {
                void create();
            }
            """;

    @Test
    void a_producer_method_building_a_use_case_through_a_constructor_reference_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                CREATE_WEDDING_COMMAND,
                """
                package app.rekord.usecase.fixture;

                import app.rekord.usecase.fixture.port.CreateWeddingCommand;

                public class CreateWedding implements CreateWeddingCommand {
                    public void create() {}
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.CreateWedding;
                import app.rekord.usecase.fixture.port.CreateWeddingCommand;
                import jakarta.enterprise.inject.Produces;
                import java.util.function.Supplier;

                public class UseCaseProducer {
                    @Produces
                    CreateWeddingCommand createWedding() {
                        Supplier<CreateWeddingCommand> factory = CreateWedding::new;
                        return factory.get();
                    }
                }
                """);

        assertBreaksA3On(fixtures, "app.rekord.application.fixture.UseCaseProducer");
    }

    @Test
    void a_producer_method_building_a_use_case_through_a_static_factory_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                CREATE_WEDDING_COMMAND,
                """
                package app.rekord.usecase.fixture;

                import app.rekord.usecase.fixture.port.CreateWeddingCommand;

                public class CreateWedding implements CreateWeddingCommand {
                    public static CreateWedding withDefaults() {
                        return new CreateWedding();
                    }

                    public void create() {}
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.CreateWedding;
                import app.rekord.usecase.fixture.port.CreateWeddingCommand;
                import jakarta.enterprise.inject.Produces;

                public class UseCaseProducer {
                    @Produces
                    CreateWeddingCommand createWedding() {
                        return CreateWedding.withDefaults();
                    }
                }
                """);

        assertBreaksA3On(fixtures, "app.rekord.application.fixture.UseCaseProducer");
    }

    @Test
    void a_producer_method_handing_on_the_injected_use_case_bean_through_its_port_passes_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                CREATE_WEDDING_COMMAND,
                """
                package app.rekord.usecase.fixture;

                import app.rekord.usecase.fixture.port.CreateWeddingCommand;

                public class CreateWedding implements CreateWeddingCommand {
                    public void create() {}
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.port.CreateWeddingCommand;
                import jakarta.enterprise.inject.Instance;
                import jakarta.enterprise.inject.Produces;

                public class CommandProducer {
                    @Produces
                    CreateWeddingCommand createWedding(Instance<app.rekord.usecase.fixture.CreateWedding> bean) {
                        return bean.get();
                    }
                }
                """);

        assertThatCode(() -> UseCaseRules.A3.check(fixtures)).doesNotThrowAnyException();
    }

    @Test
    void a_producer_field_holding_a_use_case_breaks_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                CREATE_WEDDING,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.CreateWedding;
                import jakarta.enterprise.inject.Produces;

                public class UseCaseProducer {
                    @Produces
                    CreateWedding createWedding;
                }
                """);

        assertBreaksA3On(fixtures, "app.rekord.application.fixture.UseCaseProducer");
    }

    @Test
    void a_producer_method_returning_a_port_passes_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                """
                package app.rekord.usecase.fixture.port;

                public interface WeddingClock {
                    long now();
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.port.WeddingClock;
                import jakarta.enterprise.inject.Produces;

                public class ClockProducer {
                    static class SystemWeddingClock implements WeddingClock {
                        public long now() {
                            return 0L;
                        }
                    }

                    @Produces
                    WeddingClock clock() {
                        return new SystemWeddingClock();
                    }
                }
                """);

        assertThatCode(() -> UseCaseRules.A3.check(fixtures)).doesNotThrowAnyException();
    }

    private static final String WEDDING_CLOCK =
            """
            package app.rekord.usecase.fixture.port;

            public interface WeddingClock {
                long now();
            }
            """;

    @Test
    void a_port_producer_throwing_a_use_case_package_exception_passes_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                WEDDING_CLOCK,
                """
                package app.rekord.usecase.fixture;

                public class UseCaseError extends RuntimeException {}
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.UseCaseError;
                import app.rekord.usecase.fixture.port.WeddingClock;
                import jakarta.enterprise.inject.Produces;

                public class ThrowsError {
                    @Produces
                    WeddingClock clock() {
                        if (System.getenv("X") == null) {
                            throw new UseCaseError();
                        }
                        return () -> 0L;
                    }
                }
                """);

        assertThatCode(() -> UseCaseRules.A3.check(fixtures)).doesNotThrowAnyException();
    }

    @Test
    void a_port_producer_reading_a_use_case_package_value_type_passes_a3() {
        JavaClasses fixtures = FixtureCompiler.compile(
                WEDDING_CLOCK,
                """
                package app.rekord.usecase.fixture;

                public record Settings(long offset) {
                    public static Settings defaults() {
                        return new Settings(0L);
                    }
                }
                """,
                """
                package app.rekord.application.fixture;

                import app.rekord.usecase.fixture.Settings;
                import app.rekord.usecase.fixture.port.WeddingClock;
                import jakarta.enterprise.inject.Produces;

                public class UsesSettings {
                    @Produces
                    WeddingClock clock() {
                        long offset = Settings.defaults().offset();
                        return () -> offset;
                    }
                }
                """);

        assertThatCode(() -> UseCaseRules.A3.check(fixtures)).doesNotThrowAnyException();
    }

    @Test
    void the_a3_description_names_the_producer_refusal() {
        assertThat(UseCaseRules.A3.getDescription()).contains("@Produces", "UD-15.a", "PIN-AC-0448");
    }

    @Test
    void the_suite_checks_a3() {
        assertThat(Arrays.stream(UseCaseArchitectureTest.class.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(ArchTest.class))
                        .map(UseCaseRulesTest::rule)
                        .map(ArchRule::getDescription))
                .anyMatch(description -> description.contains("A3"));
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
