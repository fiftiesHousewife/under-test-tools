package io.github.fiftieshousewife.undertesttools;

import io.github.fiftieshousewife.undertesttools.antipatterns.AntiPatternsCommand;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** The command line: one sub-command per mechanical step of under-test's journey. */
@Slf4j
public final class Main {

    static final int OK = 0;
    static final int FAILED = 1;
    static final int WRONG_ARGUMENTS = 2;

    static final String USAGE = """
        under-test-tools: the mechanical steps of under-test's journey.

        java -jar under-test-tools.jar <step> [options]

        Steps:
          anti-patterns   find the anti-patterns a program can decide in a repo's tests, and write them to
                          testing/tests.yaml and the anti_patterns section of testing/assessment.yaml
                          (java -jar under-test-tools.jar anti-patterns --help)
        """;

    private Main() {
    }

    public static void main(final String[] args) {
        final int status = run(List.of(args));
        if (status != OK) {
            System.exit(status);
        }
    }

    static int run(final List<String> args) {
        if (args.isEmpty() || "--help".equals(args.get(0))) {
            return help(args.isEmpty());
        }
        try {
            return step(args.get(0), args.subList(1, args.size()));
        } catch (IllegalArgumentException e) {
            log.error("{}", e.getMessage());
            return WRONG_ARGUMENTS;
        } catch (UncheckedIOException | IllegalStateException e) {
            log.error("{}", e.getMessage());
            return FAILED;
        }
    }

    private static int help(final boolean noStep) {
        log.info(USAGE);
        return noStep ? WRONG_ARGUMENTS : OK;
    }

    private static int step(final String name, final List<String> rest) {
        if ("anti-patterns".equals(name)) {
            return AntiPatternsCommand.run(rest);
        }
        log.error("Unknown step {}\n\n{}", name, USAGE);
        return WRONG_ARGUMENTS;
    }
}
