package io.github.fiftieshousewife.undertesttools.existingtests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// JUnit 3 tests: public void test...() methods in a class extending TestCase or one of Spring's JUnit 3 base classes,
// directly or through superclasses in the repo.
final class Junit3 {

    private static final Set<String> BASES = Set.of("TestCase", "AbstractDependencyInjectionSpringContextTests",
        "AbstractTransactionalSpringContextTests", "AbstractTransactionalDataSourceSpringContextTests",
        "AbstractJpaTests", "AbstractSpringContextTests", "AbstractJUnit38SpringContextTests",
        "AbstractTransactionalJUnit38SpringContextTests");

    private Junit3() {
    }

    static boolean extendsBase(final ClassInfo type, final Map<String, ClassInfo> bySimpleName) {
        final Set<String> seen = new HashSet<>();
        String parent = type.getSuperclass();
        while (!parent.isEmpty() && seen.add(parent)) {
            if (BASES.contains(parent)) {
                return true;
            }
            final ClassInfo next = bySimpleName.get(parent);
            parent = next == null ? "" : next.getSuperclass();
        }
        return false;
    }

    // The classes, with the test... methods of JUnit 3 classes marked as tests.
    static List<ClassInfo> withTests(final List<ClassInfo> found) {
        final Map<String, ClassInfo> bySimpleName = new HashMap<>();
        found.forEach(c -> bySimpleName.putIfAbsent(c.getSimpleName(), c));
        return found.stream().map(c -> extendsBase(c, bySimpleName) ? markTests(c) : c).toList();
    }

    private static ClassInfo markTests(final ClassInfo type) {
        final List<MethodInfo> methods = type.getMethods().stream()
            .map(m -> m.isTest() || !m.getName().startsWith("test") ? m : m.toBuilder().test(true).build())
            .toList();
        return type.toBuilder().clearMethods().methods(methods).build();
    }
}
