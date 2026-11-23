// Conventions for every Java project. The standards they enforce are listed in AGENTS.md.
// Three layers (see the decision guides on using the toolkit in an organisation):
//  - environment (proxy, package repository): never set here; comes from the organisation's Gradle init script;
//  - code-quality standards: Checkstyle, PMD (complexity, the assertAll rule), copy-paste detection, SpotBugs with
//    FindSecBugs, forbidden APIs. The toolkit's rules are defaults: `underTest.codeStyle` = TOOLKIT, ORGANISATION
//    (the organisation's rule files) or NONE (the organisation's own plugin does it);
//  - testing evidence: coverage verification (and, as they are built, flaky-test detection, mutation sampling and the
//    acceptance check for AI-written tests). Always on; only thresholds change.
// Every underTest setting can also be a Gradle property, e.g. -PunderTest.codeStyle=NONE, or in an init script.
// Out-of-date dependencies are reported by `./gradlew dependencyUpdates` (Ben Manes versions plugin), and checked in CI.
// Mutation testing is opt-in: `./gradlew pitest`.

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import groovy.json.JsonSlurper
import java.io.OutputStream
import de.thetaphi.forbiddenapis.gradle.CheckForbiddenApis
import io.github.fiftieshousewife.undertest.gradle.CodeStyleSource
import io.github.fiftieshousewife.undertest.gradle.UnderTestExtension

plugins {
    java
    jacoco
    checkstyle
    pmd
    id("com.github.spotbugs")
    id("de.thetaphi.forbiddenapis")
    id("io.github.ben-manes.versions")
    id("info.solidsoft.pitest")
}

group = "io.github.fiftieshousewife.undertesttools"

val libs = the<VersionCatalogsExtension>().named("libs")
fun version(alias: String) = libs.findVersion(alias).get().requiredVersion
fun library(alias: String) = libs.findLibrary(alias).get()
val qualityConfig = rootProject.layout.projectDirectory.dir("config/quality")

// ---------- Settings an organisation can change ----------
fun fileProperty(name: String) = providers.gradleProperty(name).map { rootProject.layout.projectDirectory.file(it) }
val underTest = extensions.create<UnderTestExtension>("underTest").apply {
    codeStyle.convention(
        providers.gradleProperty("underTest.codeStyle").map { CodeStyleSource.valueOf(it.trim().uppercase()) }
            .orElse(CodeStyleSource.TOOLKIT),
    )
    checkstyleConfig.convention(fileProperty("underTest.checkstyleConfig"))
    pmdRuleset.convention(fileProperty("underTest.pmdRuleset"))
    forbiddenApiSignatures.convention(fileProperty("underTest.forbiddenApiSignatures"))
    minLineCoverage.convention(
        providers.gradleProperty("underTest.minLineCoverage").orElse(version("min-line-coverage")).map { it.toBigDecimal() },
    )
}
val codeStyle = underTest.codeStyle

// The rule file a code-quality check uses: the toolkit's, or (with ORGANISATION) the organisation's. If the
// organisation's file is missing, the check fails when it runs, with a message saying which setting to add.
fun Task.useRuleFile(organisationFile: RegularFileProperty, toolkitFile: RegularFile, setting: String, apply: (File) -> Unit) {
    when {
        codeStyle.get() == CodeStyleSource.NONE -> Unit
        codeStyle.get() == CodeStyleSource.ORGANISATION && !organisationFile.isPresent -> {
            val message = "underTest.codeStyle is ORGANISATION, so set underTest.$setting to the organisation's file"
            doFirst { throw GradleException(message) }
        }
        codeStyle.get() == CodeStyleSource.ORGANISATION -> apply(organisationFile.get().asFile)
        else -> apply(toolkitFile.asFile)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(version("java"))
    }
}

dependencies {
    // Logging: Lombok's @Slf4j on the SLF4J API, with Log4j 2 behind it at run time.
    compileOnly(library("lombok"))
    annotationProcessor(library("lombok"))
    testCompileOnly(library("lombok"))
    testAnnotationProcessor(library("lombok"))
    implementation(library("slf4j-api"))
    runtimeOnly(library("log4j-core"))
    runtimeOnly(library("log4j-slf4j2-impl"))

    testImplementation(platform(library("junit-bom")))
    testImplementation(library("junit-jupiter"))
    testImplementation(library("assertj-core"))
    testRuntimeOnly(library("junit-platform-launcher"))

    spotbugsPlugins(library("findsecbugs"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror", "-parameters"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// ---------- Tests and coverage ----------
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = version("jacoco")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = underTest.minLineCoverage.get()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ---------- Code-quality standards (replaceable; see underTest.codeStyle) ----------
listOf(Checkstyle::class, Pmd::class, SpotBugsTask::class, CheckForbiddenApis::class).forEach { type ->
    tasks.withType(type.java).configureEach {
        // A local copy, so the configuration cache doesn't have to save a reference to this build script.
        val style = underTest.codeStyle
        onlyIf("underTest.codeStyle is not NONE") { style.get() != CodeStyleSource.NONE }
    }
}

checkstyle {
    toolVersion = version("checkstyle")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    useRuleFile(underTest.checkstyleConfig, qualityConfig.file("checkstyle/checkstyle.xml"), "checkstyleConfig") {
        configFile = it
        configDirectory = it.parentFile
    }
}

pmd {
    toolVersion = version("pmd")
    ruleSets = emptyList()
    isConsoleOutput = true
    isIgnoreFailures = false
}

tasks.withType<Pmd>().configureEach {
    useRuleFile(underTest.pmdRuleset, qualityConfig.file("pmd/ruleset.xml"), "pmdRuleset") { ruleSetFiles = files(it) }
    // The JDK's own classes, from the toolchain, so PMD resolves types against Java 25 instead of guessing.
    val jdkClasses = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(version("java")) }
        .map { it.metadata.installationPath.file("lib/jrt-fs.jar") }
    classpath = (classpath ?: files()).plus(files(jdkClasses))
}

// ---------- Copy-paste detection: PMD's own CPD, run directly (no extra plugin to mirror) ----------
val cpdTool = configurations.create("cpdTool") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies {
    cpdTool("net.sourceforge.pmd:pmd-cli:${version("pmd")}")
    cpdTool("net.sourceforge.pmd:pmd-java:${version("pmd")}")
}
val cpdCheck = tasks.register<JavaExec>("cpdCheck") {
    group = "verification"
    description = "Fails if 75 or more tokens of Java code are copy-pasted (PMD's CPD)."
    val style = underTest.codeStyle
    onlyIf("underTest.codeStyle is not NONE") { style.get() != CodeStyleSource.NONE }
    val sources = files("src/main/java", "src/test/java", "src/integrationTest/java").filter { it.isDirectory }
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    classpath = cpdTool
    mainClass = "net.sourceforge.pmd.cli.PmdCli"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(version("java")) }
    args("cpd", "--minimum-tokens", "75", "--language", "java", "--format", "text")
    argumentProviders.add(CommandLineArgumentProvider { sources.files.sorted().flatMap { listOf("--dir", it.path) } })
}
tasks.check {
    dependsOn(cpdCheck)
}

// ---------- Bugs and security bugs ----------
spotbugs {
    toolVersion = version("spotbugs")
    effort = Effort.MAX
    reportLevel = Confidence.LOW
    ignoreFailures = false
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") { required = true }
}

// ---------- Forbidden APIs ----------
forbiddenApis {
    bundledSignatures = setOf(
        "jdk-unsafe",
        "jdk-deprecated",
        "jdk-internal",
        "jdk-non-portable",
        "jdk-system-out",
        "jdk-reflection",
    )
    ignoreFailures = false
}

tasks.withType<CheckForbiddenApis>().configureEach {
    useRuleFile(underTest.forbiddenApiSignatures, qualityConfig.file("forbidden-apis/signatures.txt"), "forbiddenApiSignatures") {
        signaturesFiles = files(it)
    }
}

// ---------- Out-of-date dependencies ----------
fun isPreRelease(version: String) =
    Regex("(?i).*[.-](alpha|beta|rc|cr|m|milestone|preview|snapshot|ea)[.\\d-]*").matches(version)

tasks.withType<DependencyUpdatesTask>().configureEach {
    rejectVersionIf { isPreRelease(candidate.version) && !isPreRelease(currentVersion) }
    outputFormatter = "json,plain"
    outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.path
    reportfileName = "report"
}

// Reads the versions report. Warns about out-of-date dependencies, or fails when quality.failOnStaleDependencies=true.
val failOnStaleDependencies = providers.gradleProperty("quality.failOnStaleDependencies").map { it.toBoolean() }.orElse(false)
val dependencyReport = layout.buildDirectory.file("reports/dependencyUpdates/report.json")
val checkDependencyVersions = tasks.register("checkDependencyVersions") {
    group = "verification"
    description = "Reports out-of-date dependencies; fails if quality.failOnStaleDependencies is true."
    dependsOn(tasks.withType<DependencyUpdatesTask>())
    val report = dependencyReport
    val fail = failOnStaleDependencies
    doLast {
        @Suppress("UNCHECKED_CAST")
        val outdated = (JsonSlurper().parse(report.get().asFile) as Map<String, Any>)["outdated"] as Map<String, Any>
        val count = (outdated["count"] as Number).toInt()
        if (count > 0) {
            val message = "$count out-of-date dependencies; see ${report.get().asFile.parentFile}/report.txt"
            if (fail.get()) throw GradleException(message) else logger.warn(message)
        }
    }
}

if (providers.environmentVariable("CI").isPresent) {
    tasks.check {
        dependsOn(checkDependencyVersions)
    }
}

// ---------- Mutation testing (opt-in) ----------
pitest {
    pitestVersion = version("pitest")
    junit5PluginVersion = version("pitest-junit5")
    targetClasses = listOf("${project.group}.*")
    outputFormats = listOf("HTML", "XML")
    timestampedReports = false
    fullMutationMatrix = true // records every test that ran against each planted bug, for the summary
    verbosity = "QUIET" // the mutationSummary task below prints a short, plain-English result instead
}

// After every PIT run, print a plain-English summary instead of making people read PIT's console output.
val mutationSummary = tasks.register<io.github.fiftieshousewife.undertest.gradle.MutationSummaryTask>("mutationSummary") {
    group = "verification"
    description = "Summarises the last PIT run: coverage, planted bugs caught and missed, and where."
    reportDir = layout.buildDirectory.dir("reports/pitest")
    sourceDirs.from(sourceSets.named("main").map { it.java.srcDirs })
}
tasks.named<JavaExec>("pitest") {
    doFirst {
        // PIT's long statistics and info lines are replaced by the summary; warnings and errors still show.
        val pit = this as JavaExec
        pit.standardOutput = OutputStream.nullOutputStream()
        pit.errorOutput = io.github.fiftieshousewife.undertest.gradle.InfoLineFilter(System.err)
    }
    finalizedBy(mutationSummary)
}
