package app.rekord.application.health;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.web.health.HealthResource;
import app.rekord.architecture.FixtureCompiler;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** AC3, static half (BR-OPS-21): the health resource reaches no datasource, repository, port or EntityManager. */
class HealthResourceDependenciesTest {

    /** The contract tree, and two packages named exactly: their subpackages (java.lang.invoke, ...) stay refused. */
    private static final String[] ALLOWED = {"app.rekord.api..", "jakarta.enterprise.context", "java.lang"};

    private static ArchRule onlyTheContractAndCdi(String resourceName) {
        return ArchRuleDefinition.classes()
                .that()
                .haveFullyQualifiedName(resourceName)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(ALLOWED)
                .as(resourceName + " depends only on the generated contract, the CDI scope annotations and java.lang");
    }

    @Test
    void the_health_resource_depends_only_on_the_generated_contract_and_cdi() {
        // Given
        JavaClasses classes = new ClassFileImporter().importClasses(HealthResource.class);

        // Then
        onlyTheContractAndCdi(HealthResource.class.getName()).check(classes);
    }

    @Test
    void the_rule_refuses_a_resource_that_injects_a_datasource_or_an_entity_manager() {
        // Given a resource that does what the rule forbids
        JavaClasses classes = new ClassFileImporter().importClasses(ResourceWithDependencies.class);

        // When
        EvaluationResult result = onlyTheContractAndCdi(ResourceWithDependencies.class.getName()).evaluate(classes);

        // Then
        assertThat(result.hasViolation()).isTrue();
        String details = String.join("\n", result.getFailureReport().getDetails());
        assertThat(details).contains("javax.sql.DataSource").contains("jakarta.persistence.EntityManager");
    }

    @Test
    void the_rule_refuses_a_helper_of_the_health_package_that_could_reach_a_datasource() {
        // Given a resource in the health resource's own package that reaches a datasource through a helper there
        JavaClasses classes = FixtureCompiler.compile(
                """
                package app.rekord.adapter.web.health;
                public class DatabaseHelper {
                    @jakarta.inject.Inject
                    javax.sql.DataSource dataSource;
                }
                """,
                """
                package app.rekord.adapter.web.health;
                public class ResourceWithAHelper {
                    @jakarta.inject.Inject
                    DatabaseHelper helper;
                }
                """);

        // When
        EvaluationResult result =
                onlyTheContractAndCdi("app.rekord.adapter.web.health.ResourceWithAHelper").evaluate(classes);

        // Then
        assertThat(result.hasViolation()).isTrue();
        String details = String.join("\n", result.getFailureReport().getDetails());
        assertThat(details).contains("app.rekord.adapter.web.health.DatabaseHelper");
    }

    @Test
    void the_rule_refuses_a_reflective_lookup_through_a_java_lang_subpackage() {
        // Given a resource that looks a datasource up by name through java.lang.invoke
        JavaClasses classes = FixtureCompiler.compile(
                """
                package app.rekord.adapter.web.health;
                public class ResourceWithAReflectiveLookup {
                    Object lookup() throws Exception {
                        return java.lang.invoke.MethodHandles.lookup().findClass("javax.sql.DataSource");
                    }
                }
                """);

        // When
        EvaluationResult result =
                onlyTheContractAndCdi("app.rekord.adapter.web.health.ResourceWithAReflectiveLookup").evaluate(classes);

        // Then
        assertThat(result.hasViolation()).isTrue();
        String details = String.join("\n", result.getFailureReport().getDetails());
        assertThat(details).contains("java.lang.invoke.MethodHandles");
    }

    /** Never a bean (no scope annotation): it only gives the rule something to refuse. */
    static class ResourceWithDependencies {
        @Inject
        DataSource dataSource;

        @Inject
        EntityManager entityManager;
    }
}
