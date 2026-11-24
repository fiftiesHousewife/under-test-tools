package io.github.fiftieshousewife.undertesttools.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
          draft-the-connection-list: {version: "1.0", commit: "aaaaaaa"}
          find-test-anti-patterns: {version: "0.9", commit: "bbbbbbb"}
        tests:
          - test: "com.example.ATest.reachesTheDatabase"
            at: "src/test/java/com/example/ATest.java:5"
            connections:
              - {node: "orders-db", kind: database, found_by: code-path}
            anti_patterns:
              kinds: [reaches-external-system, sleeps]
              reaches:
                - {host: orders-db.internal, kind: database, through: code, where: "src/test/java/com/example/A.java:3"}
          - test: "com.example.ATest.wasEmpty"
            at: "src/test/java/com/example/ATest.java:12"
            anti_patterns:
              kinds: [no-real-assertion]
        """;

    @Test
    void keepsOtherSkillsFieldsAndTheKindsTheSkillDecides(@TempDir final Path dir) throws IOException {
        final Path file = Files.writeString(dir.resolve("tests.yaml"), EXISTING);
        final Finding found = new Finding("src/test/java/com/example/ATest.java:20");
        found.add("disabled");
        TestsFile.update(file, new TestsFile.Update(Map.of("com.example.ATest.skipped", found),
            new java.util.LinkedHashMap<>(Map.of("version", "1.0")), List.of()));
        final String text = Files.readString(file);
        assertAll(
            () -> assertThat(text).startsWith("# yaml-language-server: $schema=../schema/tests.schema.json\n"),
            () -> assertThat(text).contains("draft-the-connection-list: {version: \"1.0\", commit: \"aaaaaaa\"}"),
            () -> assertThat(text).contains("find-test-anti-patterns: {version: \"1.0\"}"),
            () -> assertThat(text).contains("- {node: orders-db, kind: database, found_by: code-path}"),
            () -> assertThat(text).contains("kinds: [reaches-external-system]"),
            () -> assertThat(text).contains("- {host: \"orders-db.internal\""),
            () -> assertThat(text).doesNotContain("wasEmpty"),
            () -> assertThat(text).contains("test: \"com.example.ATest.skipped\""));
    }

    @Test
    void replacesTheSectionInPlaceOrInsertsItBeforeTheNextOne() {
        final String text = "# header\nschema: assessment/1\nsafety:\n  findings: []\nanti_patterns:\n  old: 1\n"
            + "obstacles:\n  x: 1\n";
        assertAll(
            () -> assertThat(AssessmentFile.replaceSection(text, "anti_patterns", "anti_patterns:\n  new: 2\n"))
                .isEqualTo("# header\nschema: assessment/1\nsafety:\n  findings: []\nanti_patterns:\n  new: 2\n"
                    + "obstacles:\n  x: 1\n"),
            () -> assertThat(AssessmentFile.replaceSection("schema: a\ntechniques:\n  y: 2\n", "anti_patterns",
                "anti_patterns:\n  new: 2\n")).isEqualTo("schema: a\nanti_patterns:\n  new: 2\ntechniques:\n  y: 2\n"));
    }

    private static final String DECISIONS = """
        schema: assessment/1
        decisions:
          anti-patterns/sleeps:
            state: decided
            choice: kept
          anti-patterns/disabled:
            state: stale
          anti-patterns/copy-pasted:
            state: proposed
        """;

    @Test
    void decisionsGoStaleComeBackAndAreAddedButOtherwiseStay() {
        final String updated = Decisions.update(DECISIONS, List.of("disabled", "never-runs", "sleeps",
            "other-name-does-not-match-assertions"));
        assertAll(
            () -> assertThat(updated).contains("anti-patterns/sleeps:\n    state: decided"),
            () -> assertThat(updated).contains("anti-patterns/disabled:\n    state: proposed"),
            () -> assertThat(updated).contains("anti-patterns/copy-pasted:\n    state: stale"),
            () -> assertThat(updated).contains("anti-patterns/never-runs:\n    state: proposed\n    drafted_by: ai\n"
                + "    choice: \"Application Owner decides: make it a real test"),
            () -> assertThat(updated).contains("    question: \"" + Decisions.QUESTION + "\""));
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
