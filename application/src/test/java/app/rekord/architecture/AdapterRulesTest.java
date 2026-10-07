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

class AdapterRulesTest {

    private static String unit(String pkg, String name, String body) {
        return "package " + pkg + ";\npublic class " + name + " {\n" + body + "\n}\n";
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

    // ---- A4 -------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "app.rekord.adapter.web.fixture, app.rekord.adapter.persistence.fixture",
        "app.rekord.adapter.persistence.fixture, app.rekord.adapter.web.fixture"
    })
    void web_and_persistence_depending_on_each_other_breaks_a4(String originPackage, String targetPackage) {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit(targetPackage, "Target", ""), unit(originPackage, "Origin", targetPackage + ".Target field;"));

        assertBreaks(AdapterRules.A4, fixtures, "A4", originPackage + ".Origin");
    }

    @Test
    void web_depending_on_web_shared_passes_a4() {
        JavaClasses fixtures = FixtureCompiler.compile(
                unit("app.rekord.adapter.web.shared", "Helper", ""),
                unit("app.rekord.adapter.web.fixture", "Caller", "app.rekord.adapter.web.shared.Helper helper;"));

        assertPasses(AdapterRules.A4, fixtures);
    }

    // ---- A5 -------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"app.rekord.usecase.fixture", "app.rekord.adapter.persistence.fixture", "app.rekord.gateway.fixture"})
    void the_generated_model_outside_web_and_error_breaks_a5(String originPackage) {
        JavaClasses fixtures =
                FixtureCompiler.compile(unit(originPackage, "Leaky", "app.rekord.api.model.Health health;"));

        assertBreaks(AdapterRules.A5, fixtures, "A5", originPackage + ".Leaky");
    }

    @ParameterizedTest
    @CsvSource({"app.rekord.adapter.web.fixture", "app.rekord.application.error.fixture"})
    void the_generated_model_in_web_and_error_passes_a5(String originPackage) {
        JavaClasses fixtures =
                FixtureCompiler.compile(unit(originPackage, "Mapper", "app.rekord.api.model.Health health;"));

        assertPasses(AdapterRules.A5, fixtures);
    }

    // ---- A6 -------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "app.rekord.adapter.web.fixture, jakarta.persistence.EntityManager",
        "app.rekord.usecase.fixture, org.hibernate.Session",
        "app.rekord.usecase.fixture, io.quarkus.hibernate.orm.PersistenceUnit"
    })
    void a_persistence_type_outside_the_persistence_adapter_breaks_a6(String originPackage, String type) {
        JavaClasses fixtures = FixtureCompiler.compile(unit(originPackage, "Leaky", type + " field;"));

        assertBreaks(AdapterRules.A6, fixtures, "A6", originPackage + ".Leaky");
    }

    @Test
    void an_entity_outside_the_persistence_adapter_breaks_a6() {
        JavaClasses fixtures = FixtureCompiler.compile(entity("app.rekord.application.fixture"));

        assertBreaks(AdapterRules.A6, fixtures, "A6", "app.rekord.application.fixture.Guest");
    }

    @Test
    void an_entity_in_the_persistence_adapter_passes_a6() {
        JavaClasses fixtures = FixtureCompiler.compile(
                entity("app.rekord.adapter.persistence.fixture"),
                unit("app.rekord.adapter.persistence.fixture", "Repo", "jakarta.persistence.EntityManager em;"));

        assertPasses(AdapterRules.A6, fixtures);
    }

    private static String entity(String pkg) {
        return "package " + pkg + ";\n@jakarta.persistence.Entity\npublic class Guest {\n}\n";
    }

    // ---- A11 ------------------------------------------------------------------------------------------------

    private static final String HEALTH_BODY = "public app.rekord.api.model.Health health() {"
            + " return new app.rekord.api.model.Health().ok(true); }";

    @Test
    void a_resource_not_implementing_a_generated_api_interface_breaks_a11() {
        JavaClasses fixtures = FixtureCompiler.compile(
                "package app.rekord.adapter.web.fixture;\n@jakarta.ws.rs.Path(\"/loose\")\npublic class LooseResource {\n"
                        + "@jakarta.ws.rs.GET public String get() { return \"x\"; }\n}\n");

        assertBreaks(AdapterRules.A11, fixtures, "A11", "app.rekord.adapter.web.fixture.LooseResource");
    }

    @Test
    void a_resource_implementing_a_generated_api_but_declaring_its_own_path_breaks_a11() {
        JavaClasses onClass = FixtureCompiler.compile("package app.rekord.adapter.web.fixture;\n"
                + "@jakarta.ws.rs.Path(\"/own\")\npublic class PathOnClassResource implements app.rekord.api.HealthApi {\n"
                + HEALTH_BODY + "\n}\n");
        JavaClasses onMethod = FixtureCompiler.compile("package app.rekord.adapter.web.fixture;\n"
                + "public class PathOnMethodResource implements app.rekord.api.HealthApi {\n"
                + "@jakarta.ws.rs.Path(\"/own\") " + HEALTH_BODY + "\n}\n");

        assertBreaks(AdapterRules.A11, onClass, "A11", "app.rekord.adapter.web.fixture.PathOnClassResource");
        assertBreaks(AdapterRules.A11, onMethod, "A11", "app.rekord.adapter.web.fixture.PathOnMethodResource");
    }

    @Test
    void a_resource_implementing_a_generated_api_without_a_path_passes_a11() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                        "app.rekord.adapter.web.fixture", "StatusResource", "").replace(
                        "StatusResource {", "StatusResource implements app.rekord.api.HealthApi {" + HEALTH_BODY));

        assertPasses(AdapterRules.A11, fixtures);
    }

    @Test
    void a_class_named_resource_without_jax_rs_annotations_still_needs_the_interface() {
        JavaClasses fixtures = FixtureCompiler.compile(unit("app.rekord.adapter.web.fixture", "PlainResource", ""));

        assertBreaks(AdapterRules.A11, fixtures, "A11", "app.rekord.adapter.web.fixture.PlainResource");
    }

    // ---- A14 ------------------------------------------------------------------------------------------------

    private static String statusFilter(String pkg) {
        return "package " + pkg + ";\n@jakarta.ws.rs.ext.Provider\npublic class StatusFilter"
                + " implements jakarta.ws.rs.container.ContainerResponseFilter {\n"
                + "public void filter(jakarta.ws.rs.container.ContainerRequestContext request,"
                + " jakarta.ws.rs.container.ContainerResponseContext response) throws java.io.IOException {\n"
                + "response.setStatus(201);\n}\n}\n";
    }

    @Test
    void a_web_class_calling_response_status_breaks_a14() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "Creator",
                "public Object create() { return jakarta.ws.rs.core.Response.status(201).build(); }"));

        assertBreaks(AdapterRules.A14, fixtures, "A14", "app.rekord.adapter.web.fixture.Creator");
    }

    @Test
    void a_web_class_building_a_response_with_a_status_breaks_a14() {
        JavaClasses ok = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "OkBuilder",
                "public Object get() { return jakarta.ws.rs.core.Response.ok().build(); }"));
        JavaClasses builder = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "StatusBuilder",
                "public Object get(jakarta.ws.rs.core.Response.ResponseBuilder b) { return b.status(202); }"));

        assertBreaks(AdapterRules.A14, ok, "A14", "app.rekord.adapter.web.fixture.OkBuilder");
        assertBreaks(AdapterRules.A14, builder, "A14", "app.rekord.adapter.web.fixture.StatusBuilder");
    }

    @Test
    void a_web_response_filter_setting_the_status_breaks_a14() {
        JavaClasses fixtures = FixtureCompiler.compile(statusFilter("app.rekord.adapter.web.fixture"));

        assertBreaks(AdapterRules.A14, fixtures, "A14", "app.rekord.adapter.web.fixture.StatusFilter");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "org.jboss.resteasy.reactive.server.jaxrs.ContainerResponseContextImpl, setStatus(201)",
        "org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl, setStatus(201)",
        "io.vertx.core.http.impl.Http1xServerResponse, setStatusCode(201)"
    })
    void setting_the_status_through_an_implementation_type_breaks_a14(String type, String call) {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture", "Sneaky", "public void set(" + type + " target) { target." + call + "; }"));

        assertBreaks(AdapterRules.A14, fixtures, "A14", "app.rekord.adapter.web.fixture.Sneaky");
    }

    @Test
    void a_web_method_annotated_response_status_breaks_a14() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "Annotated",
                "@org.jboss.resteasy.reactive.ResponseStatus(201) public String create() { return \"\"; }"));

        assertBreaks(AdapterRules.A14, fixtures, "A14", "app.rekord.adapter.web.fixture.Annotated");
    }

    @Test
    void the_same_status_code_in_adapter_web_shared_passes_a14() {
        JavaClasses fixtures = FixtureCompiler.compile(statusFilter("app.rekord.adapter.web.shared"));

        assertPasses(AdapterRules.A14, fixtures);
    }

    @Test
    void a_web_class_only_reading_the_status_passes_a14() {
        JavaClasses fixtures = FixtureCompiler.compile(unit(
                "app.rekord.adapter.web.fixture",
                "Reader",
                "public Object read(jakarta.ws.rs.core.Response response) { return response.getStatusInfo(); }"));

        assertPasses(AdapterRules.A14, fixtures);
    }

    @Test
    void the_suite_checks_a14() {
        assertThat(Arrays.stream(AdapterArchitectureTest.class.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(ArchTest.class))
                        .map(AdapterRulesTest::rule)
                        .map(ArchRule::getDescription))
                .anyMatch(d -> d.contains("A14 ("));
    }

    // ---- suite ----------------------------------------------------------------------------------------------

    @Test
    void the_suite_checks_a4_a5_a6_a11() {
        List<String> descriptions = Arrays.stream(AdapterArchitectureTest.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ArchTest.class))
                .map(AdapterRulesTest::rule)
                .map(ArchRule::getDescription)
                .toList();

        for (String id : List.of("A4", "A5", "A6", "A11")) {
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
