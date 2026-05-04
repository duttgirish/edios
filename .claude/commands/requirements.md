---
description: Analyse a feature request or raw requirement and produce structured BRs, user stories, and acceptance criteria. Usage: /requirements [description-or-ticket]
---

Analyse `$ARGUMENTS` and produce structured requirements artefacts. Use the business-analyst agent.

## Step 1 — Extract & Clarify
Before writing artefacts, identify:
- **Actors**: who initiates, who is affected
- **Trigger**: what causes this to happen (user action, schedule, event)
- **Outcome**: what measurable change results
- **Constraints**: regulatory, performance, data retention

Surface every ambiguity as an `OPEN:` item. Do not assume answers.

## Step 2 — Business Rules
List every conditional logic item as a numbered BR:
```
BR-[NNN]: [Short name]
Condition : [When / If ...]
Action    : [System must ...]
Exception : [Unless ...]
Source    : [Policy / Stakeholder]
```

## Step 3 — User Stories
One story per user goal:
```
As a [specific role]
I want to [action]
So that [measurable outcome]

Priority : [Must / Should / Could / Won't]
Depends on: [Story IDs if any]
```

## Step 4 — Acceptance Criteria
Gherkin for each story. Minimum:
- One happy path scenario
- One validation failure scenario
- One edge case (boundary value or concurrent state)

```gherkin
Scenario: [outcome description]
  Given [system state]
  When  [actor action]
  Then  [observable result]
  And   [secondary result]
```

## Step 5 — Definition of Done Checklist
```
- [ ] All ACs pass (automated + manual)
- [ ] Business rules implemented and tested
- [ ] API contract documented
- [ ] Data dictionary updated for new fields
- [ ] Monitoring/alerting configured if applicable
- [ ] Security review for sensitive data flows
- [ ] Performance tested at [N] TPS / [N] records
```

## Output Format
Produce sections in order: BRs → Stories → ACs → DoD.
Use a table of contents if output exceeds 4 stories.
Flag `[REGULATORY]` on any rule touching financial, PII, or compliance data.
