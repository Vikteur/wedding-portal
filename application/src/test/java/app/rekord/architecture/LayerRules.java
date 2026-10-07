package app.rekord.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.List;
import java.util.Optional;

public final class LayerRules {

    private static final List<String> CONTEXT_PREFIXES = List.of(
            "app.rekord.domain.", "app.rekord.usecase.", "app.rekord.adapter.web.", "app.rekord.adapter.persistence.");

    private LayerRules() {}

    public static final ArchRule A2 = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("app.rekord.domain..")
            .layer("UseCase").definedBy("app.rekord.usecase..")
            .layer("Adapter").definedBy("app.rekord.adapter..")
            .layer("Generated").definedBy("app.rekord.api..")
            .layer("Gateway").definedBy("app.rekord.gateway..")
            .layer("Application").definedBy("app.rekord.application..")
            .layer("Logging").definedBy("app.rekord.logging..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("UseCase", "Adapter", "Gateway", "Application")
            .whereLayer("UseCase").mayOnlyBeAccessedByLayers("Adapter", "Gateway", "Application")
            .whereLayer("Generated").mayOnlyBeAccessedByLayers("Adapter", "Application")
            .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Gateway").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Logging").mayOnlyBeAccessedByLayers("Adapter", "Gateway", "Application")
            .whereLayer("Application").mayNotBeAccessedByAnyLayer()
            .because("A2 (FW-C-02): dependencies point inward: adapter and gateway to use case to domain, application on top");

    public static final ArchRule A10 = CompositeArchRule.of(
                    slices().matching("app.rekord.(*)..").should().beFreeOfCycles().allowEmptyShould(true))
            .and(slices().matching("app.rekord.domain.(*)..").should().beFreeOfCycles().allowEmptyShould(true))
            .and(slices().matching("app.rekord.usecase.(*)..").should().beFreeOfCycles().allowEmptyShould(true))
            .and(slices().matching("app.rekord.adapter.web.(*)..").should().beFreeOfCycles().allowEmptyShould(true))
            .and(slices()
                    .matching("app.rekord.adapter.persistence.(*)..")
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
            .and(classes().should(onlyReachOtherContextsThroughSharedPortsOrEvents()))
            .because("A10: contexts are isolated; they meet only in shared, through a port the origin implements"
                    + " (shape b) or a domain event a use case listens to (shape a)");

    private static ArchCondition<JavaClass> onlyReachOtherContextsThroughSharedPortsOrEvents() {
        return new ArchCondition<>("depend on another context only through shared, a port it implements or a domain event") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originContext = contextOf(origin);
                if (originContext.isEmpty()) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetContext = contextOf(target);
                    if (targetContext.isEmpty()
                            || targetContext.get().equals("shared")
                            || targetContext.get().equals(originContext.get())
                            || isPortTheOriginImplements(origin, target, targetContext.get())
                            || isEventListenedToByUseCase(origin, target)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(
                            origin,
                            origin.getName() + " (context " + originContext.get() + ") depends on " + target.getName()
                                    + " (context " + targetContext.get() + ")"));
                }
            }
        };
    }

    private static Optional<String> contextOf(JavaClass javaClass) {
        String pkg = javaClass.getPackageName() + ".";
        for (String prefix : CONTEXT_PREFIXES) {
            if (pkg.startsWith(prefix)) {
                String rest = pkg.substring(prefix.length());
                return rest.isEmpty() ? Optional.empty() : Optional.of(rest.substring(0, rest.indexOf('.')));
            }
        }
        return Optional.empty();
    }

    private static boolean isPortTheOriginImplements(JavaClass origin, JavaClass target, String targetContext) {
        return target.isInterface()
                && (target.getPackageName() + ".").startsWith("app.rekord.usecase." + targetContext + ".port.")
                && origin.isAssignableTo(target.getName());
    }

    private static boolean isEventListenedToByUseCase(JavaClass origin, JavaClass target) {
        return origin.getPackageName().startsWith("app.rekord.usecase")
                && target.getAllRawInterfaces().stream().anyMatch(LayerRules::isDomainEventMarker);
    }

    private static boolean isDomainEventMarker(JavaClass type) {
        return type.getSimpleName().equals("DomainEvent") && type.getPackageName().startsWith("app.rekord.domain");
    }
}
