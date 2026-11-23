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
            log.info(USAGE);
            return args.isEmpty() ? WRONG_ARGUMENTS : OK;
        }
        final List<String> rest = args.subList(1, args.size());
        try {
            if ("anti-patterns".equals(args.get(0))) {
                return AntiPatternsCommand.run(rest);
            }
            log.error("Unknown step {}\n\n{}", args.get(0), USAGE);
            return WRONG_ARGUMENTS;
        } catch (IllegalArgumentException e) {
            log.error("{}", e.getMessage());
            return WRONG_ARGUMENTS;
        } catch (UncheckedIOException | IllegalStateException e) {
            log.error("{}", e.getMessage());
            return FAILED;
        }
    }
}
