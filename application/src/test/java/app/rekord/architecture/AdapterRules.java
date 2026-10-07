package app.rekord.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

public final class AdapterRules {

    private static final String PATH = "jakarta.ws.rs.Path";
    private static final String HTTP_METHOD = "jakarta.ws.rs.HttpMethod";

    private AdapterRules() {}

    private static final String A4_BECAUSE = "A4 (FW-C-28; 18 C-16): adapter.web and adapter.persistence never depend on each other";

    public static final ArchRule A4 = CompositeArchRule.of(noClasses()
                    .that()
                    .resideInAPackage("app.rekord.adapter.web..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("app.rekord.adapter.persistence..")
                    .allowEmptyShould(true)
                    .because(A4_BECAUSE))
            .and(noClasses()
                    .that()
                    .resideInAPackage("app.rekord.adapter.persistence..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("app.rekord.adapter.web..")
                    .allowEmptyShould(true)
                    .because(A4_BECAUSE));

    public static final ArchRule A5 = noClasses()
            .that()
            .resideOutsideOfPackages("app.rekord.adapter.web..", "app.rekord.application.error..", "app.rekord.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("app.rekord.api..")
            .because("A5 (FW-C-27, FW-C-47): only adapter.web and application.error see the generated contract types")
            .allowEmptyShould(true);

    private static final String A6_BECAUSE =
            "A6 (FW-C-37): persistence types and entities stay in app.rekord.adapter.persistence";

    public static final ArchRule A6 = CompositeArchRule.of(noClasses()
                    .that()
                    .resideOutsideOfPackage("app.rekord.adapter.persistence..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.hibernate..", "io.quarkus.hibernate..")
                    .because(A6_BECAUSE)
                    .allowEmptyShould(true))
            .and(noClasses()
                    .that()
                    .resideOutsideOfPackage("app.rekord.adapter.persistence..")
                    .should()
                    .beAnnotatedWith("jakarta.persistence.Entity")
                    .because(A6_BECAUSE)
                    .allowEmptyShould(true));

    public static final ArchRule A11 = classes()
            .that(isResource())
            .should(implementAGeneratedApiAndDeclareNoPath())
            .because("A11 (FW-C-27): a resource implements a generated Api interface and takes path, verb and media type from it");

    private static DescribedPredicate<JavaClass> isResource() {
        return DescribedPredicate.describe(
                "are resources in app.rekord.adapter.web outside shared",
                c -> c.getPackageName().startsWith("app.rekord.adapter.web")
                        && !(c.getPackageName() + ".").startsWith("app.rekord.adapter.web.shared.")
                        && (c.getSimpleName().endsWith("Resource")
                                || c.isAnnotatedWith(PATH)
                                || c.getMethods().stream().anyMatch(AdapterRules::isEndpointMethod)));
    }

    private static boolean isEndpointMethod(JavaMethod method) {
        return method.isAnnotatedWith(PATH)
                || method.getAnnotations().stream()
                        .anyMatch(a -> a.getRawType().isAnnotatedWith(HTTP_METHOD));
    }

    private static ArchCondition<JavaClass> implementAGeneratedApiAndDeclareNoPath() {
        return new ArchCondition<>("implement a generated Api interface and declare no @Path") {
            @Override
            public void check(JavaClass resource, ConditionEvents events) {
                boolean implementsApi = resource.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getPackageName().equals("app.rekord.api")
                                && i.getSimpleName().endsWith("Api"));
                if (!implementsApi) {
                    events.add(SimpleConditionEvent.violated(
                            resource, resource.getName() + " does not implement an interface of app.rekord.api named *Api"));
                }
                if (resource.isAnnotatedWith(PATH)) {
                    events.add(SimpleConditionEvent.violated(resource, resource.getName() + " declares @Path on the class"));
                }
                for (JavaMethod method : resource.getMethods()) {
                    if (method.isAnnotatedWith(PATH)) {
                        events.add(SimpleConditionEvent.violated(
                                resource, resource.getName() + "." + method.getName() + " declares @Path"));
                    }
                }
            }
        };
    }
}
