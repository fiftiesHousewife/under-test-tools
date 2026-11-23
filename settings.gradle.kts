pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Where Gradle downloads a JDK if none is installed. Without internet access, switch downloads off
    // (org.gradle.java.installations.auto-download=false) and point Gradle at installed JDKs.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "under-test-tools"
