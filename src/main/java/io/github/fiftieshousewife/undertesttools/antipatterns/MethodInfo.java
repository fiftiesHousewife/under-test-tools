package io.github.fiftieshousewife.undertesttools.antipatterns;

import lombok.Builder;
import lombok.Value;

/** A method in test code: a test, or something that looks like one, or a helper. Lines are 1-based. */
@Value
@Builder(toBuilder = true)
public class MethodInfo {
    /** The class that declares it, fully qualified, with {@code .} before a nested class's name. */
    String className;
    String name;
    /** Path from the repo root, with {@code /}. */
    String file;
    /** The line with its name: where the list of tests says it is. */
    int nameLine;
    /** From its first annotation to its closing brace. */
    int beginLine;
    int endLine;
    /** It runs as a test: a test annotation, or a JUnit 3 test. */
    boolean test;
    /** It runs before each test of its class: {@code @Before}, {@code @BeforeEach}, or JUnit 3's {@code setUp()}. */
    boolean setUp;

    /** As JUnit's reports name it: {@code com.example.OrderTest.findsAnOrder}. */
    public String testName() {
        return className + "." + name;
    }

    public String at() {
        return file + ":" + nameLine;
    }

    boolean holds(final int line) {
        return line >= beginLine && line <= endLine;
    }
}
