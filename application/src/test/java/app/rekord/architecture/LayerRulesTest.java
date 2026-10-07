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

class LayerRulesTest {

    /** A class {@code pkg.name} whose body refers to other fixtures by their fully qualified names. */
    private static String unit(String pkg, String name, String body) {
        return "package " + pkg + ";\npublic class " + name + " {\n" + body + "\n}\n";
    }

    private static String type(String pkg, String declaration) {
        return "package " + pkg + ";\npublic " + declaration + "\n";
    }

    private static void assertBreaks(ArchRule rule, JavaClasses fixtures, String... mentioned) {
        var thrown = assertThatThrownBy(() -> rule.check(fixtures)).isInstanceOf(AssertionError.class);
        for (String text : mentioned) {
            thrown.hasMessageContaining(text);
        }
    }

    // ---- A2 -------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "app.rekord.domain.fixture, app.rekord.usecase.fixture",
        "app.rekord.usecase.fixture, app.rekord.adapter.web.fixture",
        "app.rekord.usecase.fixture, app.rekord.gateway.fixture",
        "app.rekord.adapter.web.fixture, app.rekord.gateway.fixture",
        "app.rekord.gateway.fixture, app.rekord.adapter.web.fixture",
        "app.rekord.adapter.web.fixture, app.rekord.application.fixture",
        "app.rekord.domain, app.rekord.api.fixture"
    })
    void a_forbidden_arrow_breaks_a2(String originPackage, String targetPackage) {
        JavaClasses fixtures = FixtureCompiler.compile(
                type(targetPackage, "class Target {}"),
                unit(originPackage, "Origin", targetPackage + ".Target field;"));

        assertBreaks(LayerRules.A2, fixtures, "A2", originPackage + ".Origin");
    }

    @Test
    void only_the_allowed_arrows_pass_a2() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.domain.fixture", "class DomainThing {}"),
                unit("app.rekord.usecase.fixture", "UseCaseThing", "app.rekord.domain.fixture.DomainThing d;"),
                unit(
                        "app.rekord.adapter.web.fixture",
                        "WebThing",
                        "app.rekord.usecase.fixture.UseCaseThing u; app.rekord.domain.fixture.DomainThing d;"
                                + " app.rekord.api.HealthApi api;"),
                unit(
                        "app.rekord.gateway.fixture",
                        "GatewayThing",
                        "app.rekord.usecase.fixture.UseCaseThing u; app.rekord.domain.fixture.DomainThing d;"),
                unit(
                        "app.rekord.application.fixture",
                        "ApplicationThing",
                        "app.rekord.adapter.web.fixture.WebThing w; app.rekord.gateway.fixture.GatewayThing g;"
                                + " app.rekord.usecase.fixture.UseCaseThing u; app.rekord.domain.fixture.DomainThing d;"));

        assertThatCode(() -> LayerRules.A2.check(fixtures)).doesNotThrowAnyException();
    }

    // ---- A10 ------------------------------------------------------------------------------------------------

    @Test
    void a_cycle_between_two_domain_contexts_breaks_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit("app.rekord.domain.planning", "PlanningThing", "app.rekord.domain.portal.PortalThing p;"),
                unit("app.rekord.domain.portal", "PortalThing", "app.rekord.domain.planning.PlanningThing q;"));

        assertBreaks(LayerRules.A10, fixtures);
    }

    @Test
    void a_domain_context_depending_on_another_without_a_cycle_breaks_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.domain.planning", "class PlanningThing {}"),
                unit("app.rekord.domain.portal", "PortalThing", "app.rekord.domain.planning.PlanningThing q;"));

        assertBreaks(LayerRules.A10, fixtures, "app.rekord.domain.portal.PortalThing");
    }

    @Test
    void a_use_case_depending_on_a_non_port_class_of_another_context_breaks_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.usecase.planning", "class PlanningUseCase {}"),
                unit("app.rekord.usecase.portal", "PortalUseCase", "app.rekord.usecase.planning.PlanningUseCase p;"));

        assertBreaks(LayerRules.A10, fixtures, "app.rekord.usecase.portal.PortalUseCase");
    }

    @Test
    void a_use_case_depending_on_a_domain_class_of_another_context_that_is_not_an_event_breaks_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.domain.planning", "record Wedding(String name) {}"),
                unit("app.rekord.usecase.portal", "PortalUseCase", "app.rekord.domain.planning.Wedding w;"));

        assertBreaks(LayerRules.A10, fixtures, "app.rekord.usecase.portal.PortalUseCase");
    }

    @Test
    void a_web_context_depending_on_another_web_context_breaks_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.adapter.web.planning", "class PlanningResource {}"),
                unit(
                        "app.rekord.adapter.web.portal",
                        "PortalResource",
                        "app.rekord.adapter.web.planning.PlanningResource p;"));

        assertBreaks(LayerRules.A10, fixtures, "app.rekord.adapter.web.portal.PortalResource");
    }

    @Test
    void every_context_may_depend_on_the_shared_packages_and_both_cross_context_shapes_pass_a10() {
        JavaClasses fixtures = FixtureCompiler.compile(
                type("app.rekord.domain.shared", "interface DomainEvent {}"),
                type("app.rekord.domain.shared", "record Id(String value) {}"),
                type("app.rekord.usecase.shared", "class Transactions {}"),
                type("app.rekord.domain.planning", "record WeddingCreated(app.rekord.domain.shared.Id id)"
                        + " implements app.rekord.domain.shared.DomainEvent {}"),
                type("app.rekord.usecase.planning.port", "interface PortalCommands {}"),
                unit("app.rekord.domain.portal", "PortalThing", "app.rekord.domain.shared.Id id;"),
                // shape (b): the portal use case implements a port that planning owns
                type(
                        "app.rekord.usecase.portal",
                        "class PortalUseCase implements app.rekord.usecase.planning.port.PortalCommands {"
                                + " app.rekord.usecase.shared.Transactions t; }"),
                // shape (a): a listener reacts to a domain event of another context
                unit("app.rekord.usecase.portal", "WeddingListener", "app.rekord.domain.planning.WeddingCreated e;"));

        assertThatCode(() -> LayerRules.A10.check(fixtures)).doesNotThrowAnyException();
    }

    // ---- suite ----------------------------------------------------------------------------------------------

    @Test
    void the_suite_checks_a2_and_a10() {
        List<String> descriptions = Arrays.stream(LayerArchitectureTest.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ArchTest.class))
                .map(LayerRulesTest::rule)
                .map(ArchRule::getDescription)
                .toList();

        assertThat(descriptions).anyMatch(d -> d.contains("A2"));
        assertThat(descriptions).anyMatch(d -> d.contains("A10"));
        assertThat(LayerRules.A2.getDescription()).contains("A2");
        assertThat(LayerRules.A10.getDescription()).contains("A10");
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
