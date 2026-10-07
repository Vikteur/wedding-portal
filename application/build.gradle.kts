plugins {
    java
    alias(libs.plugins.quarkus)
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(project(":rekord-domain"))
    implementation(project(":rekord-usecase"))
    implementation(project(":rekord-adapter"))
    implementation(project(":rekord-gateway"))
    implementation(project(":logging"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest-jackson")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation(libs.assertj.core)
}

tasks.test {
    systemProperty("wedding.repoRoot", rootDir.absolutePath)
    // ClassFileVersionTest reads the class directories of every module, so they must be compiled first.
    // Derived from the subprojects so a module added to settings.gradle.kts cannot be skipped.
    rootProject.subprojects.filter { it != project }.forEach {
        dependsOn("${it.path}:classes", "${it.path}:testClasses")
    }
}
