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
}
