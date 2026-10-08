plugins {
    `java-library`
    alias(libs.plugins.jandex)
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    compileOnly(platform(libs.quarkus.bom))
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    compileOnly("jakarta.transaction:jakarta.transaction-api")
    api(project(":rekord-domain"))
    testImplementation(platform(libs.quarkus.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("jakarta.enterprise:jakarta.enterprise.cdi-api")
    testImplementation("jakarta.transaction:jakarta.transaction-api")
}

// Test beans of this module for @QuarkusTest in application (architecture-conventions §13.3): a jar of the test classes
// with its own Jandex index, offered as the configuration testArtifacts. The "tests" classifier keeps it apart from the main jar.
val testSourceSet: SourceSet = sourceSets.getByName("test")
val testJandexIndex: Provider<RegularFile> = layout.buildDirectory.file("jandex-test/jandex.idx")
val jandexTool: Configuration = configurations.getByName("jandex")

val testJandex = tasks.register<org.kordamp.gradle.plugin.jandex.tasks.JandexTask>("testJandex") {
    dependsOn(tasks.named("testClasses"))
    processDefaultFileSet = false
    sources.from(testSourceSet.output.classesDirs)
    classpath = jandexTool
    destination = testJandexIndex
}

val testJar = tasks.register<Jar>("testJar") {
    archiveClassifier = "tests"
    from(testSourceSet.output)
    from(testJandex.flatMap { it.destination }) { into("META-INF") }
}

configurations.create("testArtifacts") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("testArtifacts", testJar)
}
