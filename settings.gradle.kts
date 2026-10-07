rootProject.name = "wedding-portal"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include(
    "rekord-domain",
    "rekord-usecase",
    "rekord-adapter",
    "rekord-gateway",
    "application",
    "logging",
)
