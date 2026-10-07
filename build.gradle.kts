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
            useJUnitPlatform()
            shouldRunAfter(tasks.named("test"))
        }

        // The image runs with -Duser.timezone=UTC in JAVA_OPTS; every test JVM does the same, on any runner OS.
        tasks.withType<Test>().configureEach {
            // A provider, because the Quarkus plugin reassigns jvmArgs on the application's test tasks.
            jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Duser.timezone=UTC") })
        }

        tasks.named("check") {
            dependsOn(integrationTest)
        }
    }
}
