package app.rekord.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

public final class UseCaseRules {

    /** The only two framework types a use case may depend on (CT-11, architecture-conventions §6.2). */
    static final Set<String> CT_11_ALLOWED =
            Set.of("jakarta.transaction.Transactional", "jakarta.enterprise.context.ApplicationScoped");

    /** The enum member type that ArchUnit reports for every {@code @Transactional}, whose default is TxType.REQUIRED. */
    private static final String TRANSACTIONAL_MEMBER = "jakarta.transaction.Transactional$TxType";

    private UseCaseRules() {}

    public static final ArchRule A3 = noClasses()
            .that()
            .resideInAPackage("app.rekord.usecase..")
            .should()
            .dependOnClassesThat(DescribedPredicate.not(
                            JavaClass.Predicates.resideInAnyPackage("java..", "app.rekord.domain..", "app.rekord.usecase..")
                                    .or(describe("is a CT-11 annotation", c -> CT_11_ALLOWED.contains(c.getName())
                                    || c.getName().equals(TRANSACTIONAL_MEMBER)))))
            .because("A3 (FW-C-15, FW-C-18; 18 C-06; CT-11 exception: only jakarta.transaction.Transactional and"
                    + " jakarta.enterprise.context.ApplicationScoped): a use case depends on java, domain and usecase types");
}
