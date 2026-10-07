package app.rekord.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductionCodeRulesTest {

    private static final String SECURITY_IDENTITY = "io.quarkus.security.identity.SecurityIdentity";
    private static final String REKORD_EXCEPTION =
            "package app.rekord.domain.shared.error;\npublic class RekordException extends RuntimeException {}\n";

    private static String unit(String pkg, String name, String body) {
        return "package " + pkg + ";\npublic class " + name + " {\n" + body + "\n}\n";
    }

    private static String subclass(String pkg, String name, String parent) {
        return "package " + pkg + ";\npublic class " + name + " extends " + parent + " {}\n";
    }

    private static void assertBreaks(ArchRule rule, JavaClasses fixtures, String... mentioned) {
        var thrown = assertThatThrownBy(() -> rule.check(fixtures)).isInstanceOf(AssertionError.class);
        for (String text : mentioned) {
            thrown.hasMessageContaining(text);
        }
    }

    private static void assertPasses(ArchRule rule, JavaClasses fixtures) {
        assertThatCode(() -> rule.check(fixtures)).doesNotThrowAnyException();
    }

    // ---- A7 -------------------------------------------------------------------------------------------------

    @Test
    void a_usecase_using_the_security_identity_breaks_a7() {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit("app.rekord.usecase.fixture", "Whoami", SECURITY_IDENTITY + " identity;"));

        assertBreaks(ProductionCodeRules.A7, fixtures, "A7", "app.rekord.usecase.fixture.Whoami");
    }

    @Test
    void an_application_error_class_using_the_security_identity_breaks_a7() {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit("app.rekord.application.error.fixture", "Mapper", SECURITY_IDENTITY + " identity;"));

        assertBreaks(ProductionCodeRules.A7, fixtures, "A7", "app.rekord.application.error.fixture.Mapper");
    }

    @Test
    void an_application_error_class_using_a_security_exception_passes_a7() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.application.error.fixture", "Mapper", "io.quarkus.security.ForbiddenException denied;"));

        assertPasses(ProductionCodeRules.A7, fixtures);
    }

    @ParameterizedTest
    @CsvSource({"app.rekord.adapter.web.fixture", "app.rekord.application.security.fixture"})
    void web_and_application_security_may_use_the_security_identity_in_a7(String pkg) {
        JavaClasses fixtures = FixtureCompiler.compile(unit(pkg, "Principal", SECURITY_IDENTITY + " identity;"));

        assertPasses(ProductionCodeRules.A7, fixtures);
    }

    // ---- A8 -------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "app.rekord.domain.fixture | java.time.Instant.now()",
                "app.rekord.usecase.fixture | java.time.LocalDate.now()",
                "app.rekord.domain.fixture | java.time.LocalDateTime.now()",
                "app.rekord.usecase.fixture | java.time.OffsetDateTime.now()",
                "app.rekord.domain.fixture | System.currentTimeMillis()",
                "app.rekord.usecase.fixture | java.util.UUID.randomUUID()",
                "app.rekord.domain.fixture | new java.util.Random()",
                "app.rekord.usecase.fixture | new java.security.SecureRandom()",
                "app.rekord.domain.fixture | Math.random()"
            })
    void reading_the_clock_or_randomness_directly_breaks_a8(String pkg, String call) {
        JavaClasses fixtures = FixtureCompiler.compile(unit(pkg, "Impure", "Object value() { return " + call + "; }"));

        assertBreaks(ProductionCodeRules.A8, fixtures, "A8", pkg + ".Impure");
    }

    @Test
    void reading_the_time_from_an_injected_clock_passes_a8() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.usecase.fixture",
                "Pure",
                "Object at(java.time.Clock clock) { return java.time.Instant.now(clock); }"));

        assertPasses(ProductionCodeRules.A8, fixtures);
    }

    @Test
    void an_id_generator_provider_in_application_config_may_call_random_uuid_in_a8() {
        JavaClasses fixtures = FixtureCompiler.compile(
                "package app.rekord.usecase.shared.port;\npublic interface IdGenerator { String next(); }\n",
                "package app.rekord.application.config;\npublic class SystemIdGenerator"
                        + " implements app.rekord.usecase.shared.port.IdGenerator {\n"
                        + "public String next() { return java.util.UUID.randomUUID().toString(); }\n}\n");

        assertPasses(ProductionCodeRules.A8, fixtures);
    }

    @Test
    void a_clock_provider_in_application_config_may_read_the_system_clock_in_a8() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.application.config",
                "ClockProvider",
                "public java.time.Clock clock() { return java.time.Clock.systemUTC(); }"
                        + " Object other() { return java.time.Instant.now(); }"));

        assertPasses(ProductionCodeRules.A8, fixtures);
    }

    @Test
    void the_same_call_in_a_non_provider_class_of_application_config_breaks_a8() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.application.config",
                "NotAProvider",
                "Object value() { return java.util.UUID.randomUUID(); }"));

        assertBreaks(ProductionCodeRules.A8, fixtures, "A8", "app.rekord.application.config.NotAProvider");
    }

    // ---- A9 -------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"app.rekord.adapter.web.fixture", "app.rekord.application.fixture"})
    void a_main_source_class_with_an_inject_field_breaks_a9(String pkg) {
        JavaClasses fixtures =
                FixtureCompiler.compile(unit(pkg, "FieldInjected", "@jakarta.inject.Inject String collaborator;"));

        assertBreaks(ProductionCodeRules.A9, fixtures, "A9", pkg + ".FieldInjected");
    }

    @Test
    void constructor_injection_passes_a9() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "ConstructorInjected",
                "private final String collaborator;\n"
                        + "@jakarta.inject.Inject public ConstructorInjected(String collaborator)"
                        + " { this.collaborator = collaborator; }"));

        assertPasses(ProductionCodeRules.A9, fixtures);
    }

    // ---- A12 ------------------------------------------------------------------------------------------------

    private static final String CONFIG_PROPERTY =
            "@org.eclipse.microprofile.config.inject.ConfigProperty(name = \"x\") String value;";

    private static String configMapping(String pkg) {
        return "package " + pkg + ";\n@io.smallrye.config.ConfigMapping(prefix = \"x\")\npublic interface Settings {}\n";
    }

    @Test
    void config_property_in_an_adapter_breaks_a12() {
        JavaClasses fixtures = FixtureCompiler.compile(unit("app.rekord.adapter.web.fixture", "Configured", CONFIG_PROPERTY));

        assertBreaks(ProductionCodeRules.A12, fixtures, "A12", "app.rekord.adapter.web.fixture.Configured");
    }

    @Test
    void config_mapping_in_a_usecase_breaks_a12() {
        JavaClasses fixtures = FixtureCompiler.compile(configMapping("app.rekord.usecase.fixture"));

        assertBreaks(ProductionCodeRules.A12, fixtures, "A12", "app.rekord.usecase.fixture.Settings");
    }

    @ParameterizedTest
    @CsvSource({"app.rekord.application.config", "app.rekord.gateway.fixture"})
    void config_in_application_and_gateway_passes_a12(String pkg) {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit(pkg, "Configured", CONFIG_PROPERTY), configMapping(pkg));

        assertPasses(ProductionCodeRules.A12, fixtures);
    }

    // ---- A13 ------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"app.rekord.domain.fixture", "app.rekord.usecase.fixture", "app.rekord.adapter.web.fixture"})
    void an_exception_not_extending_rekord_exception_breaks_a13(String pkg) {
        JavaClasses fixtures =
                FixtureCompiler.compile(REKORD_EXCEPTION, subclass(pkg, "LooseError", "RuntimeException"));

        assertBreaks(ProductionCodeRules.A13, fixtures, pkg + ".LooseError");
    }

    @ParameterizedTest
    @CsvSource({"app.rekord.domain.fixture", "app.rekord.usecase.fixture", "app.rekord.adapter.web.fixture"})
    void an_exception_extending_rekord_exception_passes_a13(String pkg) {
        JavaClasses fixtures = FixtureCompiler.compile(
                REKORD_EXCEPTION, subclass(pkg, "TypedError", "app.rekord.domain.shared.error.RekordException"));

        assertPasses(ProductionCodeRules.A13, fixtures);
    }

    @Test
    void a_web_application_exception_subclass_in_adapter_web_breaks_a13() {
        JavaClasses fixtures = FixtureCompiler.compile(
                subclass("app.rekord.adapter.web.fixture", "WebFailure", "jakarta.ws.rs.WebApplicationException"));

        assertBreaks(ProductionCodeRules.A13, fixtures, "app.rekord.adapter.web.fixture.WebFailure");
    }

    @Test
    void constructing_a_jax_rs_exception_in_a_usecase_breaks_a13() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.usecase.fixture", "Thrower", "Object fail() { return new jakarta.ws.rs.NotFoundException(); }"));

        assertBreaks(ProductionCodeRules.A13, fixtures, "app.rekord.usecase.fixture.Thrower");
    }

    @Test
    void a_web_application_exception_subclass_in_application_error_passes_a13() {
        JavaClasses fixtures = FixtureCompiler.compile(subclass(
                "app.rekord.application.error.fixture", "WebFailure", "jakarta.ws.rs.WebApplicationException"));

        assertPasses(ProductionCodeRules.A13, fixtures);
    }

    // ---- suite ----------------------------------------------------------------------------------------------

    @Test
    void the_suite_checks_a7_a8_a9_a12_a13() {
        List<String> descriptions = Arrays.stream(ProductionCodeArchitectureTest.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ArchTest.class))
                .map(ProductionCodeRulesTest::rule)
                .map(ArchRule::getDescription)
                .toList();

        for (String id : List.of("A7", "A8", "A9", "A12", "A13")) {
            assertThat(descriptions).as(id).anyMatch(d -> d.contains(id + " ("));
        }
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
