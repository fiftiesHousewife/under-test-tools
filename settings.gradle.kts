pluginManagement {
    // build-logic holds only how this repo is published; the code standards come from the java-conventions plugin.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal() // the java-conventions plugin's own Gradle plugins are only here
        mavenCentral()
    }
    plugins {
        // Our code-quality standards: https://github.com/fiftiesHousewife/under-test-java-conventions
        id("io.github.fiftieshousewife.java-conventions") version "0.2.0"
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
