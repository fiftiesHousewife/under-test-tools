package io.github.fiftieshousewife.undertesttools.existingtests;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every class and method in the test folders, with their lines, read without resolving types. A JUnit 3 test is a
 * public no-argument {@code void test...()} in a class that extends {@code TestCase} or one of Spring's JUnit 3 base
 * classes, directly or through superclasses in the repo.
 */
public final class SourceIndex {

    static final Set<String> TEST_ANNOTATIONS =
        Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate");

    private final List<ClassInfo> classes;
    private final int filesNotParsed;

    private SourceIndex(final List<ClassInfo> classes, final int filesNotParsed) {
        this.classes = List.copyOf(classes);
        this.filesNotParsed = filesNotParsed;
    }

    /** Reads every {@code .java} file in the folders, which are given from the repo root. */
    public static SourceIndex read(final Path repo, final List<String> folders) {
        final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        final List<ClassInfo> found = new ArrayList<>();
        int failed = 0;
        for (final String folder : folders) {
            for (final Path file : FilesEndingIn.under(repo.resolve(folder), ".java")) {
                final String name = repo.relativize(file).toString().replace('\\', '/');
                final Optional<CompilationUnit> unit = parse(parser, file);
                if (unit.isEmpty()) {
                    failed++;
                    continue;
                }
                unit.get().findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(c -> found.add(ClassReader.read(c, new ClassReader.Where(name, folder, unit.get()))));
            }
        }
        return new SourceIndex(Junit3.withTests(found), failed);
    }

    public List<ClassInfo> getClasses() {
        return classes;
    }

    public int getFilesNotParsed() {
        return filesNotParsed;
    }

    public List<MethodInfo> tests() {
        return classes.stream().flatMap(c -> c.tests().stream()).toList();
    }

    /** The class holding a line of a file: the innermost, for nested classes. */
    public Optional<ClassInfo> classAt(final String file, final int line) {
        return classes.stream().filter(c -> c.getFile().equals(file) && c.holds(line))
            .min((a, b) -> Integer.compare(a.getEndLine() - a.getBeginLine(), b.getEndLine() - b.getBeginLine()));
    }

    /** The method holding a line of a file, if any. */
    public Optional<MethodInfo> methodAt(final String file, final int line) {
        return classAt(file, line).flatMap(c -> c.getMethods().stream().filter(m -> m.holds(line)).findFirst());
    }

    /** Whether a class is a JUnit 3 test class, through its superclasses in the repo. */
    public boolean isJunit3(final ClassInfo type) {
        final Map<String, ClassInfo> bySimpleName = new HashMap<>();
        classes.forEach(c -> bySimpleName.putIfAbsent(c.getSimpleName(), c));
        return Junit3.extendsBase(type, bySimpleName);
    }


    private static Optional<CompilationUnit> parse(final JavaParser parser, final Path file) {
        try {
            final ParseResult<CompilationUnit> result = parser.parse(file);
            return result.isSuccessful() ? result.getResult() : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }

    static int line(final Node node) {
        return node.getBegin().map(p -> p.line).orElse(0);
    }

    static int endLine(final Node node) {
        return node.getEnd().map(p -> p.line).orElse(0);
    }

    static boolean annotated(final MethodDeclaration method, final Set<String> names) {
        return method.getAnnotations().stream().anyMatch(a -> names.contains(a.getName().getIdentifier()));
    }
}
