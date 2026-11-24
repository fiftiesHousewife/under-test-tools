package io.github.fiftieshousewife.undertesttools.existingtests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class FilesEndingIn {

    private FilesEndingIn() {
    }

    // Every file under the folder whose name ends with the suffix, sorted; none if the folder isn't there.
    static List<Path> under(final Path folder, final String suffix) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + folder, e);
        }
    }
}
