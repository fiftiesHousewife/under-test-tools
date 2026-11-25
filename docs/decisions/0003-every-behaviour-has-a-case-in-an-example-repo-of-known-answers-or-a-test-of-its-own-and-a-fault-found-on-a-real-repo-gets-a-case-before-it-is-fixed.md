---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# Every behaviour has a case, in an example repo of known answers or a test of its own, and a fault found on a real repo gets a case before it is fixed

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

A sub-command's output feeds later steps of the journey, where a mistake is costly to trace. Real repos are where faults show up, but they can't be kept in this repo's tests.

## Considered Options

* **An example repo per sub-command, with its known answer checked byte for byte, plus a test for each behaviour it doesn't show**  
  Every rule is pinned down; a fault seen on a real repo becomes a permanent case. The example repos have to be kept up as rules are added.
* **Tests against real public repos in the build**  
  Realistic, but slow, needing the network, and their answers change with the repos.

## Decision Outcome

Chosen: each sub-command has its own example repo in `src/test/resources/`, whose known answer the tests check, and a test of its own for any behaviour the example doesn't show. A fault found on a real repo gets a case first, which fails, and is then fixed.

### Consequences

* `sample-repo/` holds `meaningless-and-unreliable`'s cases (a copy of under-test's eval example repo); `repair-orders-repo/` holds `repair-orders`'.
* A new sub-command comes with its example repo (the add-a-sub-command skill).

## More Information

* Builds on: [The journey's mechanical steps are one command-line tool with a sub-command per step, in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0077-the-journeys-mechanical-steps-are-one-command-line-tool-with-a-sub-command-per-step-in-its-own-repo.md) (under-test)
* Later decisions that build on this: none yet
