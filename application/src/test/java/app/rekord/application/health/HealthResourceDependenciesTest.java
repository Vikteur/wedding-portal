package app.rekord.application.health;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.adapter.web.health.HealthResource;
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

    private static final String[] ALLOWED = {
        "app.rekord.api..", "app.rekord.adapter.web.health..", "jakarta.enterprise.context..", "java.lang.."
    };

    private static ArchRule onlyTheContractAndCdi(String resourceName) {
        return ArchRuleDefinition.classes()
                .that()
                .haveFullyQualifiedName(resourceName)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(ALLOWED)
                .as(resourceName + " depends only on the generated contract, CDI scopes and java.lang");
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

    /** Never a bean (no scope annotation): it only gives the rule something to refuse. */
    static class ResourceWithDependencies {
        @Inject
        DataSource dataSource;

        @Inject
        EntityManager entityManager;
    }
}
