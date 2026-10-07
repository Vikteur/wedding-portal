plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    api(project(":rekord-usecase"))
    api(project(":rekord-domain"))
    implementation(project(":logging"))
}
