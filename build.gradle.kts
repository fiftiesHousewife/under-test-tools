// under-test-tools: the mechanical steps of under-test's journey, one sub-command each. The code standards come from
// the java-conventions plugin; see AGENTS.md.

plugins {
    id("io.github.fiftieshousewife.java-conventions")
    application
    alias(libs.plugins.cyclonedx)
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

// The software bill of materials: every library the jar holds, which is its runtime class path.
val bom = tasks.named<org.cyclonedx.gradle.CyclonedxDirectTask>("cyclonedxDirectBom") {
    includeConfigs = listOf("runtimeClasspath")
    componentName = "under-test-tools"
    projectType = org.cyclonedx.model.Component.Type.APPLICATION
}

// What a release publishes, under its published names: the jar and its bill of materials, in build/release. The
// release workflow signs them and adds their checksums (see docs/decisions and maintainer-tools/release).
tasks.register<Copy>("releaseFiles") {
    group = "build"
    description = "Puts the jar and its bill of materials in build/release, named for the version."
    val version = project.version.toString()
    from(allInOne) { rename { "under-test-tools-$version.jar" } }
    from(bom.flatMap { it.jsonOutput }) { rename { "under-test-tools-$version-cyclonedx.json" } }
    into(layout.buildDirectory.dir("release"))
}
