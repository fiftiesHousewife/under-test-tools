package io.github.fiftieshousewife.undertesttools.existingtests;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * A class in test code, with its methods and what is known about it without resolving types.
 *
 * <p>{@code folder} is the folder of test code it is in, from the repo root; {@code superclass} the simple name of the
 * class it extends, or empty; {@code jupiter} says it imports JUnit 5 or 6 ({@code org.junit.jupiter}).
 */
@Value
@Builder(toBuilder = true)
public class ClassInfo {
    String name;
    String simpleName;
    String file;
    String folder;
    int beginLine;
    int endLine;
    String superclass;
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
