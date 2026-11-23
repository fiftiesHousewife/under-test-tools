// under-test-tools: the mechanical steps of under-test's journey, one sub-command each. The code standards come from
// build-logic's java-conventions; see AGENTS.md.

plugins {
    id("io.github.fiftieshousewife.java-conventions")
    application
}

dependencies {
    implementation(libs.javaparser.core)
    implementation(libs.pmd.java)
    implementation(libs.jackson3.yaml)
    implementation(libs.jackson3.databind)
}

// Built with the toolchain's JDK, but runs on Java 17 and later.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

application {
    mainClass = "io.github.fiftieshousewife.undertesttools.Main"
}

val allInOne = tasks.register<Jar>("allInOneJar") {
    group = "build"
    description = "Builds one runnable jar holding the tool and its libraries."
    archiveFileName = "under-test-tools.jar"
    manifest { attributes("Main-Class" to application.mainClass.get(), "Implementation-Version" to project.version) }
    from(sourceSets.main.map { it.output })
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/*/module-info.class", "module-info.class")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.assemble { dependsOn(allInOne) }

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = layout.projectDirectory.file("config/quality/spotbugs/exclude.xml")
}
