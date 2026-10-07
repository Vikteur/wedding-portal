plugins {
    `java-library`
}

dependencies {
    testImplementation(platform(libs.quarkus.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
