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
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-flyway-postgresql")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-narayana-jta")
    implementation("io.quarkus.security:quarkus-security")
    implementation("io.quarkus:quarkus-hibernate-validator")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.quarkus:quarkus-junit-internal")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
    testImplementation(project(path = ":rekord-adapter", configuration = "testArtifacts"))
    testImplementation(project(path = ":rekord-usecase", configuration = "testArtifacts"))
    testImplementation("io.smallrye:jandex")
}

tasks.test {
    systemProperty("wedding.repoRoot", rootDir.absolutePath)
    // GeneratedContractTest reads the spec's tags; lazily, so `help` works without the property.
    val contractSpec = providers.gradleProperty("contract.spec").map { rootProject.file(it).absolutePath }
    jvmArgumentProviders.add(CommandLineArgumentProvider { contractSpec.map { listOf("-Dcontract.spec=$it") }.getOrElse(listOf()) })
    // Tests read repository files through wedding.repoRoot (ci.yml, the Dockerfile, build scripts, code-map leaves ...),
    // so every file but build outputs and tool state is an input: an edit runs the tests again without --rerun.
    // Anchored like .gitignore, so the test package named `build` stays an input.
    inputs.files(fileTree(rootDir) {
        exclude(".git", ".git/**", ".gradle/**", ".kotlin/**", ".idea/**", "**/*.iml",
            "build/**", "*/build/**", "out/**", "*/out/**", "contract/**")
    }).withPropertyName("repoFiles").withPathSensitivity(PathSensitivity.RELATIVE)
    // The checkout contract.spec points into: the spec and the hub's smoke/pom.xml (HubProbeParityTest).
    // Derived from the property, never a path or tag; optional, so `help` and a run without it still configure.
    inputs.files(contractSpec.map { listOf(File(it), File(it).parentFile.parentFile.resolve("smoke/pom.xml")) }.orElse(listOf()))
        .withPropertyName("contractFiles").withPathSensitivity(PathSensitivity.NAME_ONLY).optional()
    // ClassFileVersionTest reads the class directories of every module, so they must be compiled first.
    // Derived from the subprojects so a module added to settings.gradle.kts cannot be skipped.
    rootProject.subprojects.filter { it != project }.forEach {
        dependsOn("${it.path}:classes", "${it.path}:testClasses")
    }
}
