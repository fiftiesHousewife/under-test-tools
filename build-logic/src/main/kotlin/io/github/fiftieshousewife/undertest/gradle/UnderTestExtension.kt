package io.github.fiftieshousewife.undertest.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Settings an organisation can change, in three layers:
 *  - environment (proxy, package repository): never set here; it comes from the organisation's Gradle init script;
 *  - code-quality standards: the toolkit's rules are defaults, replaceable or switchable off ([codeStyle]);
 *  - testing evidence (coverage, mutation sampling, ...): always on; only thresholds can change ([minLineCoverage]).
 *
 * Every value can also be set as a Gradle property (for example `underTest.codeStyle=NONE` in an init script or
 * gradle.properties), so an organisation can change all its builds without editing each repo.
 */
abstract class UnderTestExtension {

    /** Whose code-quality rules the style, complexity, copy-paste, bug and forbidden-API checks use. */
    abstract val codeStyle: Property<CodeStyleSource>

    /** With [CodeStyleSource.ORGANISATION]: the organisation's Checkstyle configuration file. */
    abstract val checkstyleConfig: RegularFileProperty

    /** With [CodeStyleSource.ORGANISATION]: the organisation's PMD rule set file. */
    abstract val pmdRuleset: RegularFileProperty

    /** With [CodeStyleSource.ORGANISATION]: extra forbidden-API signatures, in addition to the bundled JDK sets. */
    abstract val forbiddenApiSignatures: RegularFileProperty

    /** Testing evidence: smallest line coverage a project may have, as a fraction (0.80 means 80%). */
    abstract val minLineCoverage: Property<java.math.BigDecimal>
}

enum class CodeStyleSource {
    /** The toolkit's own rules, in config/quality/. */
    TOOLKIT,

    /** The toolkit's tools, with the organisation's rule files. */
    ORGANISATION,

    /** No code-quality checks from the toolkit; the organisation's own plugin provides them. */
    NONE,
}
