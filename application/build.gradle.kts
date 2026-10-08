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
    implementation("io.quarkus:quarkus-security")
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
    // so every file but build outputs, tool state and the contract checkout (contract/, whose files the tests read come
    // in as contractFiles below) is an input: an edit runs the tests again without --rerun.
    // build and out are anchored like .gitignore, so the test package named `build` stays an input; tool state is
    // excluded at any depth, as .gitignore ignores it. fileTree also drops Gradle's default excludes, among them
    // .gitignore and .gitattributes, which the build tests read: those are named on their own.
    inputs.files(fileTree(rootDir) {
        exclude(".git", ".git/**", "**/.gradle/**", "**/.kotlin/**", "**/.idea/**", "**/*.iml",
            "build/**", "*/build/**", "out/**", "*/out/**", "contract/**")
    }, rootProject.files(".gitignore", ".gitattributes")).withPropertyName("repoFiles").withPathSensitivity(PathSensitivity.RELATIVE)
    // The checkout contract.spec points into: the spec (<checkout>/dist/openapi.yaml) and the hub's smoke/pom.xml two
    // levels up, as HubProbeParityTest.hubPom() finds it. NAME_ONLY because the checkout lives in a different directory
    // on every machine: only the file names and their content count, never where the checkout is.
    // Derived from the property, never a path or tag; optional, so `help` and a run without it still configure.
    inputs.files(contractSpec.map { listOfNotNull(File(it), File(it).parentFile?.parentFile?.resolve("smoke/pom.xml")) }.orElse(listOf()))
        .withPropertyName("contractFiles").withPathSensitivity(PathSensitivity.NAME_ONLY).optional()
    // ClassFileVersionTest reads the class directories of every module, so they must be compiled first.
    // Derived from the subprojects so a module added to settings.gradle.kts cannot be skipped.
    rootProject.subprojects.filter { it != project }.forEach {
        dependsOn("${it.path}:classes", "${it.path}:testClasses")
    }
}
