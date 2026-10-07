plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    api(project(":rekord-domain"))
}
