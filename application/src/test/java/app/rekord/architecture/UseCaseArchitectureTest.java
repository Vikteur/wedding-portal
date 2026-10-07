package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class UseCaseArchitectureTest {

    @ArchTest
    static final ArchRule a3 = UseCaseRules.A3;
}
