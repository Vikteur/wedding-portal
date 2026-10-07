package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class UseCaseArchitectureTest {

    @ArchTest
    static final ArchRule a3 = FreezingArchRule.freeze(UseCaseRules.A3);
}
