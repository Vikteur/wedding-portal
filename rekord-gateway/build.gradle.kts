plugins {
    `java-library`
    alias(libs.plugins.jandex)
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    compileOnly(platform(libs.quarkus.bom))
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    api(project(":rekord-usecase"))
    api(project(":rekord-domain"))
    implementation(project(":logging"))
    testImplementation(libs.wiremock)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
