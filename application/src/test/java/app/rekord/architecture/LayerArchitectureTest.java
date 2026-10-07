package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class LayerArchitectureTest {

    @ArchTest
    static final ArchRule a2 = FreezingArchRule.freeze(LayerRules.A2);

    @ArchTest
    static final ArchRule a10 = FreezingArchRule.freeze(LayerRules.A10);
}
