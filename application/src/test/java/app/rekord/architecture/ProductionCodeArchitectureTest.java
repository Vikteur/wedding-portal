package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class ProductionCodeArchitectureTest {

    @ArchTest
    static final ArchRule a7 = ProductionCodeRules.A7;

    @ArchTest
    static final ArchRule a8 = ProductionCodeRules.A8;

    @ArchTest
    static final ArchRule a9 = ProductionCodeRules.A9;

    @ArchTest
    static final ArchRule a12 = ProductionCodeRules.A12;

    @ArchTest
    static final ArchRule a13 = ProductionCodeRules.A13;
}
