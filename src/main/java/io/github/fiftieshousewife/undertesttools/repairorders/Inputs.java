package io.github.fiftieshousewife.undertesttools.repairorders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

// The three files the orders join, read from the repo's testing folder.
record Inputs(Facts facts, Map<String, Object> tests, SystemsList systems) {

    static final String EXTERNAL_SYSTEMS = "external-systems.calm.json";

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    static Inputs read(final Path repo, final String testing) {
        final Path folder = repo.resolve(testing);
        return new Inputs(new Facts(parse(JSON, folder.resolve("external-calls.json"))),
            parse(YAML, folder.resolve("tests.yaml")),
            new SystemsList(parse(JSON, folder.resolve(EXTERNAL_SYSTEMS)),
                (testing.replace('\\', '/').replaceAll("/+$", "") + "/" + EXTERNAL_SYSTEMS).replaceFirst("^/", "")));
    }

    private static Map<String, Object> parse(final ObjectMapper mapper, final Path file) {
        final String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
        try {
            return Fields.map(mapper.readValue(text, Object.class));
        } catch (JacksonException e) {
            throw new IllegalStateException("Can't parse " + file + ": " + e.getOriginalMessage(), e);
        }
    }
}
