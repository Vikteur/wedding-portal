plugins {
    `java-library`
    alias(libs.plugins.jandex)
    alias(libs.plugins.openapi.generator)
}

dependencies {
    implementation(platform(libs.quarkus.bom))
    compileOnly(platform(libs.quarkus.bom))
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
    // The APIs the generated sources import; versions come from the BOM.
    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api("jakarta.validation:jakarta.validation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("jakarta.annotation:jakarta.annotation-api")
    api(project(":rekord-usecase"))
    api(project(":rekord-domain"))
    implementation(project(":logging"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The server interfaces come from the pinned contract (rekord-contract dist/openapi.yaml), never by hand.
// The spec is required on every compiling run: -Pcontract.spec=<rekord-contract checkout>/dist/openapi.yaml
val contractSpec: Provider<RegularFile> = rootProject.layout.projectDirectory.file(
    providers.gradleProperty("contract.spec").orElse(provider {
        throw GradleException(
            "contract.spec is not set: pass -Pcontract.spec=<rekord-contract checkout>/dist/openapi.yaml"
        )
    })
)

openApiGenerate {
    generatorName = "jaxrs-spec"
    inputSpec = contractSpec
    outputDir = layout.buildDirectory.dir("generated/openapi")
    // Emptied before each run, so a tag or schema the spec no longer has leaves no stale type to compile against.
    cleanupOutput = true
    apiPackage = "app.rekord.api"
    modelPackage = "app.rekord.api.model"
    generateApiTests = false
    generateModelTests = false
    configOptions = mapOf(
        "sourceFolder" to "src/gen/java",
        "interfaceOnly" to "true",
        "useJakartaEe" to "true",
        "returnResponse" to "false",
        "useSwaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "useTags" to "true",
    )
}

sourceSets.main {
    java.srcDir(tasks.named("openApiGenerate").map { layout.buildDirectory.dir("generated/openapi/src/gen/java") })
}
