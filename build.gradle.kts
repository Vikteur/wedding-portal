subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release = 25
            options.encoding = "UTF-8"
        }

        // Fast set: every test class not ending in IT.
        tasks.named<Test>("test") {
            useJUnitPlatform()
            exclude("**/*IT.class")
        }

        // Slow set: the same src/test classes, only those ending in IT.
        val testSourceSet = extensions.getByType<SourceSetContainer>()["test"]
        val integrationTest = tasks.register<Test>("integrationTest") {
            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath
            include("**/*IT.class")
            // QuarkusUnitTest and @QuarkusTest cannot share a JVM (ExclusivityChecker), so they run apart.
            useJUnitPlatform { excludeTags("quarkus-unit-test", "resource-test") }
            shouldRunAfter(tasks.named("test"))
        }

        // Slow set, part two: the start-up refusal tests that boot Quarkus through QuarkusUnitTest.
        val startupTest = tasks.register<Test>("startupTest") {
            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath
            include("**/*IT.class")
            useJUnitPlatform { includeTags("quarkus-unit-test") }
            shouldRunAfter(integrationTest)
        }

        // Slow set, part three: resource tests in the datasource-less profile. They run in their own JVM because the
        // Testcontainers substitutor is read from the environment, and it makes any container start fail the boot.
        val resourceTest = tasks.register<Test>("resourceTest") {
            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath
            include("**/*IT.class")
            useJUnitPlatform { includeTags("resource-test") }
            environment("TESTCONTAINERS_IMAGE_SUBSTITUTOR", "app.rekord.application.ContainerTripwire")
            shouldRunAfter(integrationTest)
        }

        // The image runs with -Duser.timezone=UTC in JAVA_OPTS; every test JVM does the same, on any runner OS.
        tasks.withType<Test>().configureEach {
            // A provider, because the Quarkus plugin reassigns jvmArgs on the application's test tasks.
            jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Duser.timezone=UTC") })
        }

        tasks.named("check") {
            dependsOn(integrationTest, startupTest, resourceTest)
        }
    }
}
