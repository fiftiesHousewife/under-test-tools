package io.github.fiftieshousewife.undertesttools.antipatterns;

import lombok.Builder;
import lombok.Value;

/**
 * A method in test code: a test, or something that looks like one, or a helper. Lines are 1-based.
 *
 * <p>{@code className} is the class that declares it, fully qualified, with {@code .} before a nested class's name;
 * {@code file} its path from the repo root, with {@code /}; {@code nameLine} the line with its name, where the list of
 * tests says it is; {@code beginLine} to {@code endLine} it runs from its first annotation to its closing brace.
 * {@code test}: it runs as a test (a test annotation, or a JUnit 3 test); {@code setUp}: it runs before each test of
 * its class ({@code @Before}, {@code @BeforeEach}, or JUnit 3's {@code setUp()}).
 */
@Value
@Builder(toBuilder = true)
public class MethodInfo {
    String className;
    String name;
    String file;
    int nameLine;
    int beginLine;
    int endLine;
    boolean test;
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
