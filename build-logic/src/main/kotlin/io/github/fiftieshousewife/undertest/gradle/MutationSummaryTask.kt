package io.github.fiftieshousewife.undertest.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Prints a plain-English summary after PIT runs: how many lines the tests ran, how many planted bugs (mutations) the
 * tests caught, and each missed one next to the line of code it changed. It replaces reading PIT's long console output.
 */
abstract class MutationSummaryTask : DefaultTask() {

    /** PIT's report folder, holding mutations.xml and index.html. */
    @get:Internal
    abstract val reportDir: DirectoryProperty

    /** The main source folders, to show the line of code each missed bug changed. */
    @get:InputFiles
    abstract val sourceDirs: ConfigurableFileCollection

    private data class Mutation(
        val status: String,
        val where: Pair<String, Int>,
        val description: String,
        val tests: List<String>,
    )

    @TaskAction
    fun summarise() {
        val dir = reportDir.get().asFile
        val xml = dir.resolve("mutations.xml")
        if (!xml.isFile) {
            logger.quiet("No PIT report at $xml")
            return
        }
        val mutations = BLOCK.findAll(xml.readText()).map { parse(it.value) }.toList()
        val caught = mutations.filter { it.status == "KILLED" || it.status == "TIMED_OUT" }
        val missed = mutations - caught.toSet()
        logger.quiet("")
        coverage(dir.resolve("index.html"))?.let { logger.quiet("Line coverage: $it") }
        logger.quiet("Planted bugs:  ${mutations.size}, caught ${caught.size}, missed ${missed.size}")
        report("Caught - a test failed, as it should:", caught, "failed")
        report("Missed - every test that ran still passed:", missed, "passed")
        logger.quiet("")
        val score = if (mutations.isEmpty()) 0 else caught.size * 100 / mutations.size
        logger.quiet("Result: the tests caught ${caught.size} of ${mutations.size} planted bugs ($score%).")
    }

    private fun report(heading: String, mutations: List<Mutation>, verb: String) {
        if (mutations.isEmpty()) {
            return
        }
        logger.quiet("")
        logger.quiet(heading)
        mutations.sortedBy { it.where.second }.forEach { mutation ->
            val code = sourceLine(mutation.where.first, mutation.where.second).take(CODE_WIDTH).padEnd(CODE_WIDTH)
            val line = mutation.where.second.toString().padEnd(3)
            logger.quiet("  line $line $code ${mutation.description.padEnd(DESCRIPTION_WIDTH)} $verb: ${names(mutation.tests)}")
        }
    }

    /** As many test names as fit in [NAMES_WIDTH] characters, then how many more, so each bug fits on one line. */
    private fun names(tests: List<String>): String {
        if (tests.isEmpty()) {
            return "no test ran this line"
        }
        val shown = mutableListOf(tests.first())
        for (test in tests.drop(1)) {
            if (shown.joinToString(", ").length + test.length + 2 > NAMES_WIDTH) {
                break
            }
            shown += test
        }
        val more = tests.size - shown.size
        return shown.joinToString(", ") + if (more > 0) " and $more more" else ""
    }

    private fun parse(block: String): Mutation {
        fun field(name: String) = Regex("<$name>([^<]*)</$name>").find(block)?.groupValues?.get(1).orEmpty()
        val status = Regex("status='([A-Z_]+)'").find(block)?.groupValues?.get(1).orEmpty()
        val tests = if (status == "KILLED") field("killingTests").ifEmpty { field("killingTest") } else field("succeedingTests")
        return Mutation(
            status = status,
            where = field("sourceFile") to field("lineNumber").toInt(),
            description = plain(field("description")),
            tests = tests.split("|").mapNotNull { TEST_METHOD.find(it)?.groupValues?.get(1) }.distinct(),
        )
    }

    private fun coverage(index: File): String? {
        val match = COVERAGE.find(if (index.isFile) index.readText() else "") ?: return null
        val (run, total) = match.destructured
        return "$run of $total lines run by the tests (${run.toInt() * 100 / total.toInt()}%)"
    }

    private fun sourceLine(fileName: String, line: Int): String =
        sourceDirs.asFileTree.matching { include("**/$fileName") }.files.firstOrNull()
            ?.readLines()?.getOrNull(line - 1)?.trim() ?: ""

    private companion object {
        const val CODE_WIDTH = 40
        const val DESCRIPTION_WIDTH = 22
        const val NAMES_WIDTH = 38
        val BLOCK = Regex("<mutation .*?</mutation>", RegexOption.DOT_MATCHES_ALL)

        /** A test's method name, without its class or package: "[method:smallOrder()]" gives "smallOrder". */
        val TEST_METHOD = Regex("\\[method:([^(\\]]+)")
        val COVERAGE = Regex("coverage_legend\">(\\d+)/(\\d+)<")
        val SYMBOLS = mapOf("multiplication" to "*", "division" to "/", "subtraction" to "-", "addition" to "+")

        /** PIT's descriptions, in plain words. */
        fun plain(description: String): String {
            Regex("Replaced integer (\\w+) with (\\w+)").matchEntire(description)?.let {
                return "${SYMBOLS[it.groupValues[1]]} became ${SYMBOLS[it.groupValues[2]]}"
            }
            Regex("Changed increment from (\\d+) to -(\\d+)").matchEntire(description)?.let {
                return "+= ${it.groupValues[1]} became -= ${it.groupValues[2]}"
            }
            return when {
                description == "changed conditional boundary" -> "boundary moved by one"
                description.startsWith("replaced int return with 0") -> "result replaced with 0"
                description.startsWith("negated conditional") -> "condition reversed"
                else -> description
            }
        }
    }
}
