package io.github.fiftieshousewife.undertesttools.existingtests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

// Reads and writes the toolkit's YAML files: the header comment naming the schema, then the top-level keys in order.
final class YamlFile {

    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    private YamlFile() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(final String text) {
        final Object root = YAML.readValue(text, Object.class);
        return root instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
    }

    static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }

    static void write(final Path file, final String header, final Map<String, Object> root) {
        final StringBuilder out = new StringBuilder(header).append('\n');
        root.entrySet().forEach(e -> YamlText.write(out, 0, e));
        try {
            final Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + file, e);
        }
    }
}
