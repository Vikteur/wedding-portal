plugins {
    `java-library`
}

dependencies {
    testImplementation(platform(libs.quarkus.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
