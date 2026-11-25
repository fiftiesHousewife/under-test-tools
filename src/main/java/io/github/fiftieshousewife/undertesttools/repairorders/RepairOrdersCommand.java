package io.github.fiftieshousewife.undertesttools.repairorders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code repair-orders}: joins {@code testing/external-calls.json}, {@code testing/tests.yaml} and
 * {@code testing/external-systems.calm.json} into {@code testing/repair-orders.json}, one work order per external
 * system whose tests repair must pair. It only joins and copies; it decides nothing a person or the repair skill
 * decides.
 */
@Slf4j
public final class RepairOrdersCommand {

    static final String USAGE = """
        java -jar under-test-tools.jar repair-orders [--repo <dir>] [--testing <folder>]

          --repo <dir>          the repo (default: the current folder)
          --testing <folder>    where testing/external-calls.json, testing/tests.yaml and
                                testing/external-systems.calm.json are, and repair-orders.json goes (default: testing)
        """;

    private static final List<String> KNOWN = List.of("--repo", "--testing");
    private static final ObjectMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private RepairOrdersCommand() {
    }

    public static int run(final List<String> args) {
        if (args.contains("--help")) {
            log.info(USAGE);
            return 0;
        }
        final Map<String, String> options = options(args);
        final Path repo = Path.of(options.getOrDefault("--repo", "."));
        final String testing = options.getOrDefault("--testing", "testing");
        final Orders.Made made = Orders.make(Inputs.read(repo, testing), version());
        write(repo.resolve(testing).resolve("repair-orders.json"), JSON.writeValueAsString(made.json()) + "\n");
        log.info("{}", made.summary());
        return 0;
    }

    private static Map<String, String> options(final List<String> args) {
        if (args.size() % 2 != 0) {
            throw new IllegalArgumentException("Each option needs a value\n\n" + USAGE);
        }
        final Map<String, String> options = new TreeMap<>();
        IntStream.iterate(0, i -> i < args.size(), i -> i + 2).forEach(i -> {
            if (!KNOWN.contains(args.get(i))) {
                throw new IllegalArgumentException("Unknown option " + args.get(i) + "\n\n" + USAGE);
            }
            options.put(args.get(i), args.get(i + 1));
        });
        return options;
    }

    private static String version() {
        return Optional.ofNullable(RepairOrdersCommand.class.getPackage().getImplementationVersion())
            .orElse("unreleased");
    }

    private static void write(final Path file, final String text) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + file, e);
        }
    }
}
