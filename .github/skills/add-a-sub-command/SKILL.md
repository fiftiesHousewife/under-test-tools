---
name: add-a-sub-command
description: Add a new sub-command to under-test-tools for one of under-test's skills' mechanical steps, with its example repo of known answers. Use when a work request or an under-test issue asks for a new step, such as "a sub-command that writes X".
---

# Adding a sub-command

A sub-command takes a skill's mechanical steps: what a program can decide the same way every time
([decision 0001](../../../docs/decisions/0001-under-test-tools-does-only-what-a-program-can-decide-the-same-way-every-time-and-the-judgement-stays-in-under-tests-skills.md)).
The skill itself stays in under-test.

## 1. Pin down the step
- Read the skill in under-test that will run it, the decision that moves the step here, and the schema of every file
  it reads and writes (under-test's `schema/`). The schema is the authority
  ([decision 0002](../../../docs/decisions/0002-each-sub-command-writes-the-toolkits-files-exactly-as-their-schemas-in-under-test-say-changing-only-the-fields-its-skill-owns.md)).
- List every rule between input and output. Where the schema or the skill leaves a rule open (an order, a tie, a
  missing field), ask the person or the session that owns under-test, and have the answer written into the schema,
  not only into the code.

## 2. Cases first
- Make its example repo: `src/test/resources/<name>-repo/`, holding the inputs as a repo would have them and the
  known answer in `expected/`. Give each rule a case in it, or a test of its own when the example can't show it
  ([decision 0003](../../../docs/decisions/0003-every-behaviour-has-a-case-in-an-example-repo-of-known-answers-or-a-test-of-its-own-and-a-fault-found-on-a-real-repo-gets-a-case-before-it-is-fixed.md)).
- Write the known answer by hand from the rules, then the tests: the answer byte for byte, the inputs in another order
  giving the same file ([decision 0004](../../../docs/decisions/0004-the-same-inputs-give-the-same-files-byte-for-byte.md)),
  each refusal (a missing file, a missing field), and the options' exit codes.

## 3. Build it
- A package of its own, `src/main/java/.../undertesttools/<name>/`, with a public `<Name>Command` whose
  `run(List<String>)` returns 0 when done and prints its usage for `--help`. It throws `IllegalArgumentException`
  for a wrong option, which `Main` turns into exit code 2, and `UncheckedIOException` or `IllegalStateException` for
  a file it can't read or use, exit code 1; the message says what is wrong. Options follow the others':
  `--repo <dir>` (default `.`), and folders relative to it.
- `Main` chooses it by name; add it to `Main`'s usage.
- Read only the repo's files, write only under its `testing/` folder, run none of its code
  ([decision 0005](../../../docs/decisions/0005-it-reads-a-repos-files-and-runs-none-of-the-repos-code-writing-only-under-the-repos-testing-folder.md)).
  Sort every list by a stated key; take nothing from the clock or the machine.
- Log one summary line at the end, which the skill can quote.
- Follow the java-conventions plugin's standards (its README; `./gradlew check` enforces them). Keep a package to 20
  classes or fewer.

## 4. Check it on real repos
Run it on at least one real public repo the journey has worked on, and check its output with under-test's validator
for the schema. A fault found there gets a case in the example repo before it is fixed.

## 5. Finish
- [README.md](../../../README.md): a section for it, saying what it reads, what it writes, and its command line.
- [AGENTS.md](../../../AGENTS.md): rows for its package and its example repo. [The design](../../../docs/design.md):
  its part.
- `./gradlew check` passes. Tell under-test's session or issue the command, the version it will ship in, and its log
  line, so the skill can run it.
