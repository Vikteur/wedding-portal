package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class AdapterArchitectureTest {

    @ArchTest
    static final ArchRule a4 = FreezingArchRule.freeze(AdapterRules.A4);

    @ArchTest
    static final ArchRule a5 = FreezingArchRule.freeze(AdapterRules.A5);

    @ArchTest
    static final ArchRule a6 = FreezingArchRule.freeze(AdapterRules.A6);

    @ArchTest
    static final ArchRule a11 = FreezingArchRule.freeze(AdapterRules.A11);

    @ArchTest
    static final ArchRule a14 = FreezingArchRule.freeze(AdapterRules.A14);
}
