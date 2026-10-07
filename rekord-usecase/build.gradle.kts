plugins {
    `java-library`
    alias(libs.plugins.jandex)
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    compileOnly(platform(libs.quarkus.bom))
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    api(project(":rekord-domain"))
}
