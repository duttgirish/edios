# Claude Code — Agents & Commands Guide

A guide for developers and business analysts on how to load and use the
shared Claude Code agents and slash commands in any project.

---

## Prerequisites

Install Claude Code CLI (one-time):

```bash
npm install -g @anthropic-ai/claude-code
```

Verify installation:

```bash
claude --version
```

You need an Anthropic API key or a Claude.ai Pro/Team subscription.
Log in once:

```bash
claude
```

---

## Setup: Two Options

### Option A — Project-level (recommended for teams)

Copy the `.claude/` folder from this repository into the root of your project:

```bash
# From inside your project root
cp -r /path/to/edios/.claude .
```

Or clone and copy directly:

```bash
git clone https://github.com/duttgirish/edios.git /tmp/edios
cp -r /tmp/edios/.claude /your/project/root/
```

The agents and commands are now active whenever you run `claude` inside that project.

---

### Option B — Global (available in every project on your machine)

```bash
mkdir -p ~/.claude/agents ~/.claude/commands
cp /path/to/edios/.claude/agents/*.md ~/.claude/agents/
cp /path/to/edios/.claude/commands/*.md ~/.claude/commands/
```

Global files are loaded in every project automatically — no per-project setup needed.

---

## What Is Installed

```
.claude/
├── agents/                          ← auto-selected by Claude based on context
│   ├── developer-java.md            ← Java, Spring Boot, Quarkus, JPA, Maven/Gradle
│   ├── developer-reactive-vertx.md  ← Vert.x, Mutiny, Project Reactor, RxJava
│   ├── developer-python-spark.md    ← Python, PySpark, Delta Lake, Airflow
│   └── business-analyst.md          ← Requirements, user stories, BRs, API contracts
│
└── commands/                        ← invoked manually with /command-name
    ├── review                        ← code review with file:line citations
    ├── test                          ← generate tests (JUnit 5, pytest, PySpark)
    ├── api-design                    ← design or document REST / event contracts
    ├── requirements                  ← BRs + user stories + ACs from a description
    ├── user-stories                  ← story decomposition with Gherkin ACs
    └── refactor                      ← targeted refactor without behaviour change
```

---

## How Agents Work (Automatic)

Agents are **subagents** — Claude delegates to them automatically based on
what you ask. You do not invoke them by name.

Examples of automatic delegation:

| What you ask | Agent Claude uses |
|---|---|
| "Add a new JPA repository for the Order entity" | developer-java |
| "This Vert.x verticle is blocking the event loop, fix it" | developer-reactive-vertx |
| "Rewrite this PySpark job to remove UDFs" | developer-python-spark |
| "Write user stories for the payment feature" | business-analyst |
| "Generate acceptance criteria for transaction flagging" | business-analyst |

Just describe what you want in plain language — Claude routes to the right agent.

---

## How Commands Work (Manual)

Slash commands are invoked explicitly. Type `/command-name` followed by
your input. Run `claude` to start a session, then:

### /review

Review code for correctness, security, performance, and style.

```
/review                              # reviews git staged diff
/review src/main/java/OrderService.java
/review src/pipelines/transform.py
```

Output includes Critical / Major / Minor findings with file:line citations.

---

### /test

Generate tests for a class, method, or module.

```
/test OrderService
/test src/main/java/org/iki/engine/CelRuleEngine.java
/test src/pipelines/transaction_transform.py
```

Generates JUnit 5 + Mockito for Java, pytest for Python, StepVerifier for
Reactor, and local Spark session tests for PySpark.

---

### /api-design

Design a new API contract or document an existing endpoint.

```
/api-design payment refund feature
/api-design src/main/java/org/iki/rest/EventResource.java
```

Output: REST contract with request/response schemas, business rules applied,
events published, and error envelope definitions.

---

### /requirements

Turn a raw feature description into structured business rules, user stories,
and acceptance criteria.

```
/requirements "Users must be able to flag suspicious transactions for review"
/requirements payment-feature-brief.txt
```

Output: numbered BRs → user stories → Gherkin ACs → Definition of Done checklist.

---

### /user-stories

Decompose an epic or feature into sized, testable user stories.

```
/user-stories fraud detection module
/user-stories "compliance officer needs to approve flagged transactions"
```

Output: stories with priority, size estimate, ACs, out-of-scope list, and
open questions.

---

### /refactor

Refactor code with a specific goal, without changing behaviour.

```
/refactor src/main/java/OrderService.java extract method
/refactor src/verticle/TransactionVerticle.java reactive
/refactor src/pipelines/etl.py spark
/refactor src/main/java/TransactionEvent.java records
```

Supported goals: `extract method`, `extract class`, `flatten`, `reactive`,
`records`, `virtual threads`, `streams`, `spark`, `readability`, `performance`.

---

## Workflow Examples

### Developer: implement and test a new feature

```
# 1. Describe what to build — agent is selected automatically
You: Add a REST endpoint POST /transactions/{id}/flag that sets status=FLAGGED

# 2. Generate tests for what was built
You: /test TransactionResource

# 3. Review before raising a PR
You: /review
```

---

### Business Analyst: requirements to stories

```
# 1. Start from a raw brief
You: /requirements "Compliance team needs to review all transactions above £25,000
     before they are processed"

# 2. Expand into stories
You: /user-stories compliance review workflow

# 3. Design the API the feature needs
You: /api-design compliance review approval endpoint
```

---

### Both: refactor legacy code

```
# 1. Identify the problem
You: This service uses Thread.sleep and blocking JDBC — fix it for Vert.x

# 2. Targeted refactor
You: /refactor src/main/java/LegacyPaymentService.java reactive

# 3. Verify correctness
You: /test LegacyPaymentService
You: /review src/main/java/LegacyPaymentService.java
```

---

## Tips

- **Be specific in your ask** — the more context you give, the better the output.
  "Fix the service" is weak. "The `processPayment` method blocks the Vert.x event
  loop on the database call at line 47 — convert it to a reactive Mutiny pipeline"
  gets a precise answer.

- **Chain commands** — use `/requirements` to produce stories, then ask Claude
  to implement one story, then use `/test` and `/review` to validate.

- **Agents are context-aware** — they read your `pom.xml`, `build.gradle`, or
  `requirements.txt` to detect your Java version, framework, and libraries.
  Keep those files accurate.

- **No version lock** — agents work with any version of Claude (Haiku, Sonnet,
  Opus) and adapt to whatever framework versions are in your project.

---

## Keeping Agents Up To Date

Pull the latest agents from the source repository:

```bash
# Project-level update
git -C /tmp/edios pull origin main
cp /tmp/edios/.claude/agents/*.md .claude/agents/
cp /tmp/edios/.claude/commands/*.md .claude/commands/

# Global update
cp /tmp/edios/.claude/agents/*.md ~/.claude/agents/
cp /tmp/edios/.claude/commands/*.md ~/.claude/commands/
```

---

## Source Repository

https://github.com/duttgirish/edios
