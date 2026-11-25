---
name: write-a-decision-record
description: Draft or replace a decision record in docs/decisions/ of this repo. Use for decision items, open questions, or whenever a conversation settles a choice about how under-test-tools works or how this repo works.
---

# Writing a decision record

Records follow [MADR](https://adr.github.io/madr/); the architecture-decision-records skill, installed by APM, has the
mechanics. Every settled choice gets a record, in the same piece of work.

1. **Whose decision is it?** Which steps of the journey under-test-tools takes on, and the schemas of the files it
   writes, are under-test's decisions: propose them there. Records here are about how this tool does its work and
   how this repo works.
2. Copy [the template](../../../docs/decisions/template.md) to `docs/decisions/NNNN-<the-decision-in-kebab-case>.md`,
   numbered after the last record. The title states the decision in full, in plain English. One small decision.
3. Front matter: `status` (`proposed` until the maintainers choose it), `decision-makers: under-test-tools
   maintainers`, and a `group` already used in [the index](../../../docs/decisions/README.md).
4. **Context** is facts only. **Considered Options** are the real ones, the chosen one first, each with what follows
   from it, good and bad. **Consequences** name the files, tasks, tests and skills affected.
5. **More Information** links by title to the records this builds on, here or in under-test.
6. **An accepted record is never edited.** A change is a new record that replaces it; the old one's status says
   `replaced by NNNN`.
7. Add the record's row to [the index](../../../docs/decisions/README.md), under its group, with its status. If it
   settles a decision item, set that item's `decision_record:` in
   [plan/under-test-tools-backlog.yaml](../../../plan/under-test-tools-backlog.yaml).
8. Keep [the design](../../../docs/design.md) and [AGENTS.md](../../../AGENTS.md) in step in the same change.

## Discussing a decision with a person
Use the grilling skill (installed by APM). In Claude Code, ask **one question per AskUserQuestion call**, with the
recommended option first. Write answers into the record only once the person confirms them, and mark it `accepted`
only when they chose it.
