---
description: Generate user stories with acceptance criteria from a feature, epic, or business requirement. Usage: /user-stories [feature-description-or-epic]
---

Generate user stories for `$ARGUMENTS`. Use the business-analyst agent.

## Process
1. Identify all distinct actors who interact with this feature
2. Map each actor × goal pair to one story
3. Write ACs in Gherkin covering happy path + at least one failure path
4. Flag any assumption as `ASSUME:` and any blocker as `OPEN:`

## Story Format
```
## [Story ID]: [Title — action + object]

**As a** [specific role, not "user"]
**I want to** [concrete action]
**So that** [measurable business outcome]

**Priority**: [Must Have | Should Have | Could Have | Won't Have]
**Size**: [XS | S | M | L | XL]

### Acceptance Criteria

**Scenario: [happy path name]**
Given [precondition]
When  [action]
Then  [outcome]

**Scenario: [validation failure]**
Given [precondition]
When  [invalid action]
Then  [error shown / rejected]

### Out of Scope
- [Explicitly excluded to prevent scope creep]

### Dependencies
- [Story IDs or systems this story depends on]
```

## Epic Decomposition Rules
- One story = one user goal completable in one sprint
- Split when story has "and also" in the "I want to" clause
- Extract technical enablers as separate stories with a `[TECH]` prefix
- Never include implementation detail in the story title or body

## Sizing Guide
| Size | Criteria |
|------|---------|
| XS | Single field/view change, no new logic |
| S | One new endpoint or screen, clear bounded scope |
| M | Multiple endpoints or screens, some new business logic |
| L | New subsystem or integration, multiple actors |
| XL | Split this story further — too large |

## Output
Produce stories as a numbered list. Group by actor if multiple actors present.
End with an `OPEN QUESTIONS` section listing anything that blocks finalisation.
