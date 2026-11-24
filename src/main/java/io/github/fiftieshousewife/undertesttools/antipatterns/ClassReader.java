package io.github.fiftieshousewife.undertesttools.antipatterns;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

// Turns one parsed class into a ClassInfo.
final class ClassReader {

    private static final java.util.Set<String> SET_UP = java.util.Set.of("Before", "BeforeEach");

    /** Where a class was found: its file and test folder from the repo root, and the file's parsed form. */
    record Where(String file, String folder, CompilationUnit unit) {
    }

    private ClassReader() {
    }

    static ClassInfo read(final ClassOrInterfaceDeclaration type, final Where where) {
        final ClassInfo.ClassInfoBuilder info = ClassInfo.builder()
            .name(type.getFullyQualifiedName().orElse(type.getNameAsString()))
            .simpleName(type.getNameAsString())
            .file(where.file())
            .folder(where.folder())
            .beginLine(SourceIndex.line(type))
            .endLine(SourceIndex.endLine(type))
            .superclass(type.getExtendedTypes().stream().findFirst().map(ClassOrInterfaceType::getNameAsString)
                .orElse(""))
            .jupiter(where.unit().getImports().stream()
                .anyMatch(i -> i.getNameAsString().startsWith("org.junit.jupiter")));
        final String className = type.getFullyQualifiedName().orElse(type.getNameAsString());
        type.getMethods().forEach(method -> info.method(method(className, method, where)));
        return info.build();
    }

    private static MethodInfo method(final String className, final MethodDeclaration method, final Where where) {
        return MethodInfo.builder()
            .className(className)
            .name(method.getNameAsString())
            .file(where.file())
            .nameLine(SourceIndex.line(method.getName()))
            .beginLine(SourceIndex.line(method))
            .endLine(SourceIndex.endLine(method))
            .test(SourceIndex.annotated(method, SourceIndex.TEST_ANNOTATIONS))
            .setUp(SourceIndex.annotated(method, SET_UP) || "setUp".equals(method.getNameAsString()))
            .build();
    }
}
