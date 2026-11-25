---
status: accepted
decision-makers: under-test-tools maintainers
group: Why it exists
---

# under-test-tools does only what a program can decide the same way every time, and the judgement stays in under-test's skills

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

under-test's skills were first written as instructions an AI assistant followed by hand. Where a step is mechanical (listing tests, giving each hit of a rule to its test, counting, joining files), an assistant is slower than a program and comes out differently from run to run. Where a step needs judgement (is this test really meaningless, which stand-in suits this system), a program can't do it.

## Considered Options

* **A sub-command takes a skill's mechanical steps; the skill runs it and keeps the judgement**  
  Each step is fast and gives the same answer every run; each skill gets shorter. A skill and its sub-command must agree on where the line falls.
* **Skills keep every step**  
  Nothing to build or release, but runs differ and take longer.
* **A program takes the whole step, judgement included, with an AI model inside it**  
  One place, but the program is no longer deterministic, and needs a model wherever it runs.

## Decision Outcome

Chosen: a sub-command takes only the steps a program can decide the same way every time. Whatever needs judgement stays in the skill in under-test, and this repo holds no skills of the toolkit's own.

### Consequences

* A step moves here only when it can be specified completely: its inputs, its output, and every rule between them.
* No AI model is put inside a sub-command.
* The toolkit's skills (under-test's `.github/skills/`) stay in under-test; this repo's skills are only for working on this repo.

## More Information

* Builds on: [A skill's mechanical steps are done by deterministic tools, and the skill keeps only judgement](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0075-a-skills-mechanical-steps-are-done-by-deterministic-tools-and-the-skill-keeps-only-judgement.md) (under-test); [The journey's mechanical steps are one command-line tool with a sub-command per step, in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0077-the-journeys-mechanical-steps-are-one-command-line-tool-with-a-sub-command-per-step-in-its-own-repo.md) (under-test)
* Later decisions that build on this: none yet
