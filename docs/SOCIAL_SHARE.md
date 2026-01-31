# EDIOS - Social Sharing Templates

Use these templates to share the project across platforms.

---

## LinkedIn Post

```
I'm excited to share EDIOS - an open-source, high-throughput transaction event ingestion system I built using modern Java technologies.

Key highlights:
- Quarkus 3.x with RESTEasy Reactive for non-blocking REST endpoints
- Google CEL (Common Expression Language) for real-time rule evaluation
- Vert.x Event Bus for async event-driven processing
- Java 21 Virtual Threads for efficient concurrency
- Thread-safe rule cache with atomic volatile swap
- 129 unit tests covering all edge cases
- GraalVM native image support

The system processes financial transaction events in real-time, evaluating them against configurable CEL rules for fraud detection, compliance monitoring, and alerting.

Architecture: POST /events → Vert.x Event Bus → Virtual Thread Workers → CEL Rule Engine → ALERT

Tech stack: Java 21 | Quarkus 3.17 | Google CEL | Vert.x | MariaDB | GraalVM

Check it out: https://github.com/duttgirish/edios

#Java #Quarkus #OpenSource #Microservices #EventDriven #FraudDetection #CEL #Vertx #Java21 #VirtualThreads #ReactiveArchitecture #SoftwareEngineering
```

---

## Twitter/X Post

```
🚀 Just open-sourced EDIOS - a high-throughput transaction event ingestion system

⚡ Quarkus 3.x + Google CEL + Vert.x Event Bus
☕ Java 21 Virtual Threads
🔍 Real-time rule evaluation for fraud detection
✅ 129 tests, production-ready

https://github.com/duttgirish/edios

#Java #Quarkus #OpenSource #CEL
```

---

## Reddit (r/java, r/programming, r/microservices)

```
Title: EDIOS - Open-source high-throughput transaction event ingestion with Google CEL rule evaluation (Quarkus + Vert.x + Java 21)

Body:
I built EDIOS (Event-Driven Ingestion & Observability System) - an open-source system for real-time transaction monitoring using modern Java.

What it does:
- Accepts batches of transaction events via REST API
- Dispatches them asynchronously via Vert.x Event Bus
- Evaluates each transaction against configurable CEL (Common Expression Language) rules
- Generates alerts for matched rules (fraud detection, compliance, etc.)

Tech decisions:
- Quarkus 3.17 for fast startup and low memory footprint
- RESTEasy Reactive for non-blocking HTTP handling
- Google CEL for safe, sandboxed expression evaluation (no arbitrary code execution)
- Vert.x Event Bus for decoupled async processing
- Java 21 Virtual Threads for efficient blocking operations
- Thread-safe rule cache with volatile atomic swap (no locks)
- Immutable Java records for all data models

Performance design:
- REST endpoint returns 202 immediately, processes async
- CEL expressions pre-compiled at cache refresh (not per-request)
- Worker pool on virtual threads scales efficiently
- Local-only event bus delivery (no serialization overhead)

129 unit tests covering boundary values, thread safety, codec round-trips, and all 8 sample rules.

GitHub: https://github.com/duttgirish/edios

Feedback welcome - especially on the CEL integration patterns and the volatile swap approach for the rule cache.
```

---

## Dev.to / Hashnode / Medium Article Outline

```
Title: Building a High-Throughput Transaction Monitoring System with Quarkus, Google CEL, and Vert.x

Sections:
1. Problem: Real-time transaction rule evaluation at scale
2. Architecture Overview (use draw.io diagrams from docs/)
3. Why Google CEL? Safe expression evaluation without arbitrary code execution
4. Event-Driven Design with Vert.x Event Bus
5. Thread-Safe Rule Cache with Volatile Atomic Swap
6. Java 21 Virtual Threads in Production
7. Testing Strategy: 129 tests for a ~1000 LOC codebase
8. Performance Considerations
9. Getting Started
10. What's Next: Database integration, metrics, alerting

Link: https://github.com/duttgirish/edios
```

---

## Hacker News

```
Title: Show HN: EDIOS – High-throughput transaction event ingestion with Google CEL rule evaluation

URL: https://github.com/duttgirish/edios
```

---

## Platforms to Share On

1. **LinkedIn** - Professional network, great for Java/enterprise visibility
2. **Twitter/X** - Tech community, use hashtags
3. **Reddit** - r/java, r/programming, r/microservices, r/quarkus
4. **Dev.to** - Write a technical article with architecture details
5. **Hashnode** - Developer blogging platform
6. **Medium** - Write under Java or Microservices publications
7. **Hacker News** - "Show HN" post
8. **Quarkus Community** - https://github.com/quarkusio/quarkus/discussions
9. **Google CEL Discussions** - https://github.com/google/cel-spec/discussions
10. **DZone** - Submit as an article for Java Zone
