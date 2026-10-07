package app.rekord.architecture;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

public final class ProductionCodeRules {

    private static final String A7_BECAUSE =
            "A7 (FW-C-20, FW-C-32): the security identity is read only in adapter.web and application.security;"
                    + " application.error may use the security exception types";
    private static final String A13_BECAUSE =
            "A13 (FW-C-10, FW-C-30): errors are RekordException subtypes; only application.error knows the JAX-RS exceptions";

    private static final String WEB_APPLICATION_EXCEPTION = "jakarta.ws.rs.WebApplicationException";

    private static final Set<String> CLOCK_TYPES = Set.of(
            "java.time.Instant",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.OffsetDateTime",
            "java.time.OffsetTime",
            "java.time.ZonedDateTime");

    private static final Set<String> RANDOM_TYPES = Set.of("java.util.Random", "java.security.SecureRandom");

    private static final Set<String> PROVIDED_PORTS = Set.of("IdGenerator", "TokenGenerator");

    private ProductionCodeRules() {}

    public static final ArchRule A7 = CompositeArchRule.of(noClasses()
                    .that()
                    .resideOutsideOfPackages(
                            "app.rekord.adapter.web..", "app.rekord.application.security..", "app.rekord.application.error..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("io.quarkus.security..")
                    .because(A7_BECAUSE)
                    .allowEmptyShould(true))
            .and(noClasses()
                    .that()
                    .resideInAPackage("app.rekord.application.error..")
                    .should()
                    .dependOnClassesThat(JavaClass.Predicates.resideInAPackage("io.quarkus.security..")
                            .and(DescribedPredicate.not(assignableTo("java.lang.Throwable"))))
                    .because(A7_BECAUSE)
                    .allowEmptyShould(true));

    public static final ArchRule A8 = classes()
            .that(isNoClockOrIdProvider())
            .should(notReadTheClockOrRandomnessDirectly())
            .because("A8 (FW-C-59; 18 C-13): time and ids come from an injected Clock and the id ports;"
                    + " only the providers in app.rekord.application.config read the system")
            .allowEmptyShould(true);

    public static final ArchRule A9 = noFields()
            .should()
            .beAnnotatedWith("jakarta.inject.Inject")
            .because("A9 (FW-C-57, FW-C-15): collaborators arrive through the constructor, never through an @Inject field")
            .allowEmptyShould(true);

    public static final ArchRule A12 = noClasses()
            .that()
            .resideOutsideOfPackages("app.rekord.application..", "app.rekord.gateway..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.eclipse.microprofile.config..", "io.smallrye.config..")
            .because("A12 (FW-C-58): configuration is read only in app.rekord.application and app.rekord.gateway")
            .allowEmptyShould(true);

    public static final ArchRule A13 = CompositeArchRule.of(classes()
                    .that()
                    .resideInAnyPackage("app.rekord.domain..", "app.rekord.usecase..", "app.rekord.adapter.web..")
                    .and()
                    .areAssignableTo("java.lang.Throwable")
                    .should()
                    .beAssignableTo("app.rekord.domain.shared.error.RekordException")
                    .because(A13_BECAUSE)
                    .allowEmptyShould(true))
            .and(noClasses()
                    .that()
                    .resideOutsideOfPackage("app.rekord.application..")
                    .should()
                    .beAssignableTo(WEB_APPLICATION_EXCEPTION)
                    .because(A13_BECAUSE)
                    .allowEmptyShould(true))
            .and(noClasses()
                    .that()
                    .resideOutsideOfPackage("app.rekord.application..")
                    .should()
                    .callConstructorWhere(target(owner(assignableTo(WEB_APPLICATION_EXCEPTION))))
                    .because(A13_BECAUSE)
                    .allowEmptyShould(true));

    private static DescribedPredicate<JavaClass> isNoClockOrIdProvider() {
        return DescribedPredicate.describe(
                "are not the clock and id providers of app.rekord.application.config",
                c -> !((c.getPackageName() + ".").startsWith("app.rekord.application.config.")
                        && (c.getAllRawInterfaces().stream()
                                        .anyMatch(i -> PROVIDED_PORTS.contains(i.getSimpleName())
                                                && i.getPackageName().equals("app.rekord.usecase.shared.port"))
                                || c.getMethods().stream()
                                        .anyMatch(m -> m.getRawReturnType().getName().equals("java.time.Clock")))));
    }

    private static ArchCondition<JavaClass> notReadTheClockOrRandomnessDirectly() {
        return new ArchCondition<>("not read the clock or randomness directly") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                    if (readsSystem(call)) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " calls " + call.getTargetOwner().getName() + "." + call.getName()
                                        + " in " + call.getSourceCodeLocation()));
                    }
                }
                for (JavaConstructorCall call : javaClass.getConstructorCallsFromSelf()) {
                    if (RANDOM_TYPES.contains(call.getTargetOwner().getName())) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " creates a " + call.getTargetOwner().getName() + " in "
                                        + call.getSourceCodeLocation()));
                    }
                }
            }
        };
    }

    private static boolean readsSystem(JavaMethodCall call) {
        String owner = call.getTargetOwner().getName();
        String name = call.getName();
        return (CLOCK_TYPES.contains(owner) && name.equals("now") && call.getTarget().getRawParameterTypes().isEmpty())
                || (owner.equals("java.lang.System") && name.equals("currentTimeMillis"))
                || (owner.equals("java.util.UUID") && name.equals("randomUUID"))
                || (owner.equals("java.lang.Math") && name.equals("random"))
                || (owner.equals("java.util.concurrent.ThreadLocalRandom") && name.equals("current"));
    }
}
