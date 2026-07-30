# Domain Docs

This repository uses a single domain context.

## Before exploring

- Read `CONTEXT.md` at the repository root.
- Read ADRs under `docs/adr/` that affect the area being changed.
- If either is absent, proceed silently; domain-modeling skills create them lazily when decisions are resolved.

## Use the glossary vocabulary

Use the canonical terms defined in `CONTEXT.md` in issue titles, specifications, tests and implementation discussions. Avoid synonyms explicitly listed under `_Avoid_`.

If a required domain concept is missing, either reconsider the proposed terminology or capture the gap for a domain-modeling session.

## Respect ADRs

If proposed work contradicts an existing ADR, surface the conflict explicitly instead of silently overriding the decision.

## Layout

```text
/
├── CONTEXT.md
└── docs/
    └── adr/
```
