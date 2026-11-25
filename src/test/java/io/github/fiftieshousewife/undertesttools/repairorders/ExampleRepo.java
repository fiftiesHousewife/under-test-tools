package io.github.fiftieshousewife.undertesttools.repairorders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// repair-orders' example repo in src/test/resources: its three inputs, and the known answer.
final class ExampleRepo {

    static final Path EXAMPLE = Path.of("src/test/resources/repair-orders-repo");

    private ExampleRepo() {
    }

    // Copies the three inputs into the repo's testing folder, and gives that folder.
    static Path copy(final Path repo) throws IOException {
        final Path testing = Files.createDirectories(repo.resolve("testing"));
        copyInput("external-calls.json", testing);
        copyInput("tests.yaml", testing);
        copyInput("external-systems.calm.json", testing);
        return testing;
    }

    static String expected() throws IOException {
        return Files.readString(EXAMPLE.resolve("expected/repair-orders.json"));
    }

    private static void copyInput(final String input, final Path testing) throws IOException {
        Files.copy(EXAMPLE.resolve("testing").resolve(input), testing.resolve(input));
    }
}
