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
