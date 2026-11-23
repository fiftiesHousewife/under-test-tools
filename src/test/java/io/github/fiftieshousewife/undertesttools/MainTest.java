package io.github.fiftieshousewife.undertesttools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @Test
    void choosesTheStepAndExplainsMistakes(@TempDir final Path dir) {
        assertAll(
            () -> assertThat(Main.run(List.of())).isEqualTo(Main.WRONG_ARGUMENTS),
            () -> assertThat(Main.run(List.of("--help"))).isEqualTo(Main.OK),
            () -> assertThat(Main.run(List.of("tidy-up"))).isEqualTo(Main.WRONG_ARGUMENTS),
            () -> assertThat(Main.run(List.of("anti-patterns", "--colour", "red"))).isEqualTo(Main.WRONG_ARGUMENTS),
            () -> assertThat(Main.run(List.of("anti-patterns", "--repo", dir.toString()))).isEqualTo(Main.FAILED));
    }
}
