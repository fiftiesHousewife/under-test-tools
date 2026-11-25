package io.github.fiftieshousewife.undertesttools;

import io.github.fiftieshousewife.undertesttools.existingtests.MeaninglessAndUnreliableCommand;
import io.github.fiftieshousewife.undertesttools.repairorders.RepairOrdersCommand;
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
          meaningless-and-unreliable
                find the meaningless tests (they never run, can't fail, or check only their mocks) and unreliable
                tests (they sleep, or depend on the order tests run in) a program can decide in a repo's tests, and
                write them to testing/tests.yaml and the meaningless_tests and unreliable_tests sections of
                testing/assessment.yaml (java -jar under-test-tools.jar meaningless-and-unreliable --help)
          repair-orders
                join testing/external-calls.json, testing/tests.yaml and testing/external-systems.calm.json into
                testing/repair-orders.json, one work order per external system whose tests repair must pair
                (java -jar under-test-tools.jar repair-orders --help)
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
        if ("meaningless-and-unreliable".equals(name)) {
            return MeaninglessAndUnreliableCommand.run(rest);
        }
        if ("repair-orders".equals(name)) {
            return RepairOrdersCommand.run(rest);
        }
        log.error("Unknown step {}\n\n{}", name, USAGE);
        return WRONG_ARGUMENTS;
    }
}
