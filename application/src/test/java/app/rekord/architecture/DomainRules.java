package app.rekord.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

public final class DomainRules {

    private DomainRules() {}

    public static final ArchRule A1 = noClasses()
            .that()
            .resideInAPackage("app.rekord.domain..")
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages("java..", "app.rekord.domain..")
            .because("A1 (FW-C-01): the domain depends only on java and domain types, never on a framework or another layer");
}
