package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** A class in test code, with its methods and what is known about it without resolving types. */
@Value
@Builder(toBuilder = true)
public class ClassInfo {
    String name;
    String simpleName;
    String file;
    /** The folder of test code it is in, from the repo root. */
    String folder;
    int beginLine;
    int endLine;
    /** The simple name of the class it extends, or empty. */
    String superclass;
    /** It imports JUnit 5 or 6 (org.junit.jupiter). */
    boolean jupiter;
    @Singular
    List<MethodInfo> methods;

    List<MethodInfo> tests() {
        return methods.stream().filter(MethodInfo::isTest).toList();
    }

    boolean holds(final int line) {
        return line >= beginLine && line <= endLine;
    }
}
