package io.github.fiftieshousewife.undertesttools.existingtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The list of tests and the assessment file keep everything this tool doesn't own. */
class FilesTest {

    private static final String EXISTING = """
        # yaml-language-server: $schema=../schema/tests.schema.json
        schema: tests/1
        found_by:
          draft-the-external-systems-list: {version: "1.0", commit: "aaaaaaa"}
          find-meaningless-and-unreliable-tests: {version: "0.9", commit: "bbbbbbb"}
        tests:
          - test: "com.example.ATest.bendsItsExpectedValue"
            at: "src/test/java/com/example/ATest.java:9"
            meaningless: [no-real-assertion, other-bent-expected-value]
          - test: "com.example.ATest.reachesTheDatabase"
            at: "src/test/java/com/example/ATest.java:5"
            external_systems:
              - {node: "orders-db", kind: database, found_by: code-path}
            unreliable: [sleeps]
          - test: "com.example.ATest.wasEmpty"
            at: "src/test/java/com/example/ATest.java:12"
            meaningless: [no-real-assertion]
        """;

    private static final String UPDATED = """
        # yaml-language-server: $schema=../schema/tests.schema.json
        schema: "tests/1"
        found_by:
          draft-the-external-systems-list: {version: "1.0", commit: "aaaaaaa"}
          find-meaningless-and-unreliable-tests: {version: "1.0"}
        tests:
          - test: "com.example.ATest.bendsItsExpectedValue"
            at: "src/test/java/com/example/ATest.java:10"
            meaningless: [swallows-failures, other-bent-expected-value]
          - test: "com.example.ATest.reachesTheDatabase"
            at: "src/test/java/com/example/ATest.java:5"
            external_systems:
              - {node: orders-db, kind: database, found_by: code-path}
          - test: "com.example.ATest.waits"
            at: "src/test/java/com/example/ATest.java:20"
            meaningless: [no-real-assertion]
            unreliable: [sleeps, order-dependent]
        """;

    @Test
    void keepsOtherSkillsFieldsAndTheKindsTheSkillDecides(@TempDir final Path dir) throws IOException {
        final Path file = Files.writeString(dir.resolve("tests.yaml"), EXISTING);
        TestsFile.update(file, new TestsFile.Update(Map.of(
            "com.example.ATest.bendsItsExpectedValue", found("src/test/java/com/example/ATest.java:10",
                "swallows-failures"),
            "com.example.ATest.waits", found("src/test/java/com/example/ATest.java:20", "sleeps",
                "no-real-assertion", "order-dependent")),
            new LinkedHashMap<>(Map.of("version", "1.0")), List.of()));
        assertThat(Files.readString(file)).isEqualTo(UPDATED);
    }

    private static Finding found(final String at, final String... kinds) {
        final Finding finding = new Finding(at);
        List.of(kinds).forEach(finding::add);
        return finding;
    }

    @Test
    void replacesTheSectionsInPlaceOrInsertsThemBeforeTheNextOne() {
        final String text = "# header\nschema: assessment/1\nmeaningless_tests:\n  old: 1\nobstacles:\n  x: 1\n";
        assertAll(
            () -> assertThat(AssessmentFile.replaceSection(text, "meaningless_tests", "meaningless_tests:\n  new: 2\n"))
                .isEqualTo("# header\nschema: assessment/1\nmeaningless_tests:\n  new: 2\nobstacles:\n  x: 1\n"),
            () -> assertThat(AssessmentFile.replaceSection(text, "unreliable_tests", "unreliable_tests:\n  new: 3\n"))
                .isEqualTo("# header\nschema: assessment/1\nmeaningless_tests:\n  old: 1\nunreliable_tests:\n"
                    + "  new: 3\nobstacles:\n  x: 1\n"),
            () -> assertThat(AssessmentFile.replaceSection("schema: a\ntechniques:\n  y: 2\n", "meaningless_tests",
                "meaningless_tests:\n  new: 2\n")).isEqualTo("schema: a\nmeaningless_tests:\n  new: 2\ntechniques:\n"
                    + "  y: 2\n"));
    }

    private static final String DECISIONS = """
        schema: assessment/1
        decisions:
          unreliable/sleeps:
            state: decided
            choice: kept
          meaningless/disabled:
            state: stale
          meaningless/mocks-everything:
            state: proposed
          anti-patterns/copy-pasted:
            state: proposed
        """;

    @Test
    void decisionsGoStaleComeBackAndAreAddedButOtherwiseStay() {
        final String updated = Decisions.update(DECISIONS, List.of("meaningless/disabled", "meaningless/never-runs",
            "meaningless/other-name-does-not-match-assertions", "unreliable/sleeps", "unreliable/order-dependent"));
        assertAll(
            () -> assertThat(updated).contains("unreliable/sleeps:\n    state: decided"),
            () -> assertThat(updated).contains("meaningless/disabled:\n    state: proposed"),
            () -> assertThat(updated).contains("meaningless/mocks-everything:\n    state: stale"),
            () -> assertThat(updated).contains("anti-patterns/copy-pasted:\n    state: proposed"),
            () -> assertThat(updated).contains("meaningless/never-runs:\n    state: proposed\n    drafted_by: ai\n"
                + "    choice: \"Application Owner decides: make it a real test"),
            () -> assertThat(updated).contains("    question: \"" + Decisions.QUESTION + "\""),
            () -> assertThat(updated).endsWith("unreliable/order-dependent:\n    state: proposed\n    drafted_by: ai\n"
                + "    choice: \"fixed in the fix-flaky-tests workstream\"\n"));
    }

    @Test
    void ordersEachListsKindsAsTheSkillsTableDoes() {
        assertAll(
            () -> assertThat(TestList.MEANINGLESS.ordered(List.of("other-zzz", "sleeps", "other-bent-expected-value",
                "no-real-assertion", "never-runs"))).containsExactly("never-runs", "no-real-assertion",
                "other-bent-expected-value", "other-zzz"),
            () -> assertThat(TestList.UNRELIABLE.ordered(List.of("sleeps", "disabled", "order-dependent")))
                .containsExactly("sleeps", "order-dependent"));
    }

    @Test
    void writesTheSkillsYamlStyle() {
        assertAll(
            () -> assertThat(YamlText.scalar("never-runs")).isEqualTo("never-runs"),
            () -> assertThat(YamlText.scalar("1a2b3c4")).isEqualTo("\"1a2b3c4\""),
            () -> assertThat(YamlText.scalar("say \"hi\"")).isEqualTo("\"say \\\"hi\\\"\""),
            () -> assertThat(YamlText.scalar(3)).isEqualTo("3"),
            () -> assertThat(YamlText.scalar(null)).isEqualTo("null"),
            () -> assertThat(YamlText.inline(Map.of("a", List.of("x")))).isEqualTo("{a: [x]}"));
    }
}
