package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class ProductionCodeArchitectureTest {

    @ArchTest
    static final ArchRule a7 = FreezingArchRule.freeze(ProductionCodeRules.A7);

    @ArchTest
    static final ArchRule a8 = FreezingArchRule.freeze(ProductionCodeRules.A8);

    @ArchTest
    static final ArchRule a9 = FreezingArchRule.freeze(ProductionCodeRules.A9);

    @ArchTest
    static final ArchRule a12 = FreezingArchRule.freeze(ProductionCodeRules.A12);

    @ArchTest
    static final ArchRule a13 = FreezingArchRule.freeze(ProductionCodeRules.A13);
}
