package app.rekord.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(locations = ProductionClasses.class)
class LayerArchitectureTest {

    @ArchTest
    static final ArchRule a2 = LayerRules.A2;

    @ArchTest
    static final ArchRule a10 = LayerRules.A10;
}
