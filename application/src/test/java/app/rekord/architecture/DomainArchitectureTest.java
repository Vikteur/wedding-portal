package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule a1 = FreezingArchRule.freeze(DomainRules.A1);
}
