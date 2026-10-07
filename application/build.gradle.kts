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

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation(libs.assertj.core)
}

tasks.test {
    systemProperty("wedding.repoRoot", rootDir.absolutePath)
}

// ClassFileVersionTest reads the class directories of every module, so they must be compiled first.
tasks.test {
    listOf("rekord-domain", "rekord-usecase", "rekord-adapter", "rekord-gateway", "logging").forEach {
        dependsOn(":$it:classes", ":$it:testClasses")
    }
}
