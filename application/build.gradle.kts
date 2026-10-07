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

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.quarkus:quarkus-junit-internal")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
}

tasks.test {
    systemProperty("wedding.repoRoot", rootDir.absolutePath)
    // GeneratedContractTest reads the spec's tags; lazily, so `help` works without the property.
    val contractSpec = providers.gradleProperty("contract.spec").map { rootProject.file(it).absolutePath }
    jvmArgumentProviders.add(CommandLineArgumentProvider { contractSpec.map { listOf("-Dcontract.spec=$it") }.getOrElse(listOf()) })
    // ClassFileVersionTest reads the class directories of every module, so they must be compiled first.
    // Derived from the subprojects so a module added to settings.gradle.kts cannot be skipped.
    rootProject.subprojects.filter { it != project }.forEach {
        dependsOn("${it.path}:classes", "${it.path}:testClasses")
    }
}
