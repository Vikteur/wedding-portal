plugins {
    `java-library`
    alias(libs.plugins.jandex)
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    compileOnly(platform(libs.quarkus.bom))
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    api(project(":rekord-domain"))
    testImplementation(platform(libs.quarkus.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    testCompileOnly("jakarta.transaction:jakarta.transaction-api")
}
