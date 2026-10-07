package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule a1 = DomainRules.A1;
}
