package app.rekord.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

public final class UseCaseRules {

    /** The only two framework types a use case may depend on (CT-11, architecture-conventions §6.2). */
    static final Set<String> CT_11_ALLOWED =
            Set.of("jakarta.transaction.Transactional", "jakarta.enterprise.context.ApplicationScoped");

    /** The enum member type that ArchUnit reports for every {@code @Transactional}, whose default is TxType.REQUIRED. */
    private static final String TRANSACTIONAL_MEMBER = "jakarta.transaction.Transactional$TxType";

    private UseCaseRules() {}

    private static final String PRODUCES = "jakarta.enterprise.inject.Produces";

    private static final String PRODUCER_BECAUSE = "A3 (UD-15.a, PIN-AC-0448; architecture-conventions section 6.2):"
            + " a use case is an @ApplicationScoped bean, never the result of an @Produces method or field,"
            + " because ArC binds @Transactional to managed beans only";

    /** A use case is any usecase type except the ports, which stay producible (section 6.3). */
    private static final DescribedPredicate<JavaClass> USE_CASE_TYPE =
            JavaClass.Predicates.resideInAPackage("app.rekord.usecase..")
                    .and(JavaClass.Predicates.resideOutsideOfPackage("app.rekord.usecase..port.."))
                    .as("a use-case type");

    private static final ArchCondition<JavaMethod> RETURN_OR_BUILD_A_USE_CASE =
            new ArchCondition<>("return or construct a use-case type") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    boolean returns = USE_CASE_TYPE.test(method.getRawReturnType());
                    boolean builds = method.getConstructorCallsFromSelf().stream()
                            .anyMatch(call -> USE_CASE_TYPE.test(call.getTargetOwner()));
                    // noMethods() negates the condition: a satisfied event is the violation
                    if (returns || builds) {
                        events.add(SimpleConditionEvent.satisfied(
                                method, method.getDescription() + " returns or constructs a use-case type"));
                    }
                }
            };

    public static final ArchRule A3 = CompositeArchRule.of(noClasses()
            .that()
            .resideInAPackage("app.rekord.usecase..")
            .should()
            .dependOnClassesThat(DescribedPredicate.not(
                            JavaClass.Predicates.resideInAnyPackage("java..", "app.rekord.domain..", "app.rekord.usecase..")
                                    .or(describe("is a CT-11 annotation", c -> CT_11_ALLOWED.contains(c.getName())
                                    || c.getName().equals(TRANSACTIONAL_MEMBER)))))
            .because("A3 (FW-C-15, FW-C-18; 18 C-06; CT-11 exception: only jakarta.transaction.Transactional and"
                    + " jakarta.enterprise.context.ApplicationScoped): a use case depends on java, domain and usecase types"))
            .and(noMethods()
                    .that()
                    .areAnnotatedWith(PRODUCES)
                    .should(RETURN_OR_BUILD_A_USE_CASE)
                    .because(PRODUCER_BECAUSE)
                    .allowEmptyShould(true))
            .and(noFields()
                    .that()
                    .areAnnotatedWith(PRODUCES)
                    .should()
                    .haveRawType(USE_CASE_TYPE)
                    .because(PRODUCER_BECAUSE)
                    .allowEmptyShould(true));
}
