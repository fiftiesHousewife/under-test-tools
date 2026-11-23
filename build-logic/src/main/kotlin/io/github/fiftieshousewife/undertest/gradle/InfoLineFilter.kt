package io.github.fiftieshousewife.undertest.gradle

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

/** Passes output through line by line, dropping PIT's "INFO" lines so only warnings and errors reach the console. */
class InfoLineFilter(private val target: PrintStream) : OutputStream() {
    private val line = ByteArrayOutputStream()

    override fun write(byte: Int) {
        line.write(byte)
        if (byte == '\n'.code) {
            flushLine()
        }
    }

    override fun flush() {
        target.flush()
    }

    override fun close() {
        flushLine()
        target.flush()
    }

    private fun flushLine() {
        val text = line.toString(Charsets.UTF_8)
        line.reset()
        if (text.isNotEmpty() && !text.contains(" PIT >> INFO ")) {
            target.print(text)
        }
    }
}
