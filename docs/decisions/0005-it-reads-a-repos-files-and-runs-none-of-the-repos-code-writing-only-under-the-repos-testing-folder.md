---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# It reads a repo's files and runs none of the repo's code, writing only under the repo's testing folder

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

The journey works on old code whose tests may reach real systems. Running that code is a decision for other steps of the journey, with their own safeguards (such as the airgap agent). The tool is run inside organisations, often on the repo's own machine.

## Considered Options

* **Read only, writing only under `testing/`**  
  Safe to run anywhere at any point of the journey; it can't change the code or reach a system. What it can learn is limited to what the files say.
* **Run the repo's build or tests to learn more**  
  More facts, but every run becomes as risky as running the tests.

## Decision Outcome

Chosen: a sub-command reads the repo's files and the toolkit's files, writes only under the repo's `testing/` folder (or the folder `--testing` names), and never runs, loads or builds the repo's code. Facts that need running code come from other tools' files, such as under-test-external-calls' `testing/external-calls.json`.

### Consequences

* PMD and JavaParser parse the sources; they don't compile or load them.
* The tool needs no network access.

## More Information

* Builds on: [under-test-tools does only what a program can decide the same way every time, and the judgement stays in under-test's skills](0001-under-test-tools-does-only-what-a-program-can-decide-the-same-way-every-time-and-the-judgement-stays-in-under-tests-skills.md)
* Later decisions that build on this: none yet
