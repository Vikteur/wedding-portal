package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class AdapterArchitectureTest {

    @ArchTest
    static final ArchRule a4 = AdapterRules.A4;

    @ArchTest
    static final ArchRule a5 = AdapterRules.A5;

    @ArchTest
    static final ArchRule a6 = AdapterRules.A6;

    @ArchTest
    static final ArchRule a11 = AdapterRules.A11;
}
