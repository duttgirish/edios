---
title: Building a High-Throughput Transaction Monitoring System with Quarkus, Google CEL, and Vert.x
published: false
description: How I built EDIOS - an event-driven system for real-time transaction rule evaluation using Java 21, Quarkus 3.x, Google CEL, and Vert.x Event Bus
tags: java, quarkus, microservices, architecture
cover_image:
canonical_url: https://github.com/duttgirish/edios
---

# Building a High-Throughput Transaction Monitoring System with Quarkus, Google CEL, and Vert.x

When financial institutions process millions of transactions daily, they need real-time rule evaluation to flag suspicious activity — without slowing down the pipeline. I built **EDIOS** (Event-Driven Ingestion & Observability System) to solve exactly that.

In this article, I'll walk through the architecture, key design decisions, and code patterns behind a production-grade transaction monitoring system built with modern Java.

**GitHub**: [github.com/duttgirish/edios](https://github.com/duttgirish/edios)

---

## The Problem

Transaction monitoring systems need to:

- Accept high volumes of events without blocking callers
- Evaluate each transaction against dozens of configurable rules
- Flag matches instantly for compliance and fraud teams
- Allow rules to be updated without restarting the system
- Stay fast under load — sub-millisecond rule evaluation

Building this with traditional request-response architectures hits a wall quickly. Every transaction blocks a thread while rules are evaluated. Scale up the rules or the volume, and you're burning through thread pools.

---

## The Architecture

EDIOS decouples ingestion from processing using an event-driven architecture:

```
POST /events → REST Layer → Vert.x Event Bus → Worker Verticles → CEL Engine → ALERTS
     (202)       (validate)     (async dispatch)   (virtual threads)  (evaluate)    (log)
```

The key insight: **the REST endpoint returns 202 Accepted immediately**. The caller doesn't wait for rule evaluation. Events are dispatched to the Vert.x Event Bus and processed asynchronously on virtual threads.

### Tech Stack

| Layer | Technology | Why |
|-------|------------|-----|
| Framework | Quarkus 3.17 | Fast startup, low memory, native image support |
| REST | RESTEasy Reactive | Non-blocking HTTP handling |
| Async | Vert.x Event Bus | In-process message passing, no external broker needed |
| Rules | Google CEL 0.11.1 | Safe, sandboxed expression evaluation |
| Concurrency | Java 21 Virtual Threads | Millions of concurrent tasks without thread pool tuning |
| Data | Immutable Java Records | Thread-safe by default |

---

## Why Google CEL for Rule Evaluation?

I evaluated several options for the rule engine:

| Option | Problem |
|--------|---------|
| Drools | Heavy, complex for simple boolean rules |
| JavaScript (GraalJS) | Security risk — arbitrary code execution |
| Custom parser | Maintenance burden, bugs |
| **Google CEL** | **Safe, fast, purpose-built for boolean expressions** |

[Google CEL](https://github.com/google/cel-spec) (Common Expression Language) is designed for exactly this use case. It evaluates expressions that return `true` or `false`, with no side effects, no I/O, and no loops. You can't write `System.exit(0)` in a CEL expression — it's sandboxed by design.

Here's what CEL rules look like in EDIOS:

```cel
// Flag transactions over $10,000
amount > 10000.0

// Detect self-transfers (potential money laundering)
debitAccount == creditAccount

// Suspicious account prefix
debitAccount.startsWith("SUSP-") || creditAccount.startsWith("SUSP-")

// Round amount detection (potential structuring)
amount == double(int(amount)) && amount >= 1000.0

// Combined: large non-VIP inter-account transfer
amount > 25000.0 && debitAccount != creditAccount && !cin.startsWith("VIP-")
```

Each transaction exposes these variables to the CEL context:

| Variable | Type | Source |
|----------|------|--------|
| `debitAccount` | STRING | Source account |
| `creditAccount` | STRING | Destination account |
| `cin` | STRING | Customer ID |
| `amount` | DOUBLE | Transaction amount |
| `transactedTimeEpochSeconds` | INT (64-bit) | Unix timestamp |

### Pre-compilation for Performance

The critical optimization: **CEL expressions are compiled once at cache refresh, not on every evaluation**. The compiled `Program` objects are cached and reused across millions of evaluations.

```java
public int compileAndCacheRules(List<Rule> rules) {
    ConcurrentHashMap<Long, CelRuntime.Program> newPrograms = new ConcurrentHashMap<>();

    for (Rule rule : rules) {
        try {
            CelAbstractSyntaxTree ast = compiler.compile(rule.expression()).getAst();
            CelRuntime.Program program = runtime.createProgram(ast);
            newPrograms.put(rule.id(), program);
        } catch (CelValidationException e) {
            LOG.errorf("Failed to compile rule %d: %s", rule.id(), e.getMessage());
            // Bad rules are skipped, others still work
        }
    }

    // Atomic swap — readers never see a partially updated map
    compiledPrograms = newPrograms;
    return newPrograms.size();
}
```

At evaluation time, building the variable map and calling `program.eval()` is fast:

```java
Map<String, Object> variables = Map.of(
    "debitAccount", event.debitAccount(),
    "creditAccount", event.creditAccount(),
    "cin", event.cin(),
    "amount", event.amount().doubleValue(),
    "transactedTimeEpochSeconds", event.transactedTime().getEpochSecond()
);

Object result = program.eval(variables);
boolean matched = Boolean.TRUE.equals(result);
```

---

## Thread-Safe Rule Cache with Volatile Swap

The rule cache is the most contention-prone component. Multiple worker threads read rules while periodic refresh writes new rules. Getting this wrong means data races or stale reads.

I considered three approaches:

### Option 1: synchronized — Too slow
```java
// Every read takes a lock. Under high throughput, this serializes all workers.
synchronized List<Rule> getCachedRules() { return rules; }
```

### Option 2: ReentrantReadWriteLock — Overly complex
```java
// Read lock for reads, write lock for writes. Works, but adds complexity.
readLock.lock(); try { return rules; } finally { readLock.unlock(); }
```

### Option 3: Volatile atomic swap — Perfect fit ✓

The compiled programs map uses a `volatile` reference. Writes create an entirely new map and swap the reference in a single operation. Readers see either the old map or the new map — never a half-updated one.

```java
// Volatile reference — writes are immediately visible to all threads
private volatile Map<Long, CelRuntime.Program> compiledPrograms = new ConcurrentHashMap<>();

// Write path: build new map, then swap
ConcurrentHashMap<Long, CelRuntime.Program> newPrograms = new ConcurrentHashMap<>();
// ... populate newPrograms ...
compiledPrograms = newPrograms; // Single atomic reference write

// Read path: snapshot the reference
Map<Long, CelRuntime.Program> programs = this.compiledPrograms; // Single atomic read
CelRuntime.Program program = programs.get(rule.id());
```

The cached rule list uses `AtomicReference` with `List.copyOf()` for the same pattern:

```java
private final AtomicReference<List<Rule>> cachedRules = new AtomicReference<>(Collections.emptyList());

// Write: immutable copy, atomic set
List<Rule> immutableRules = List.copyOf(rules);
cachedRules.set(immutableRules);

// Read: no locks needed
public List<Rule> getCachedRules() {
    return cachedRules.get();
}
```

No locks, no contention, no stale reads. The pattern works because:
- Rule lists are small (tens to hundreds of rules)
- Refreshes are infrequent (every 60 seconds)
- Reads vastly outnumber writes

---

## Event-Driven Processing with Vert.x

The REST endpoint dispatches events to the Vert.x Event Bus instead of processing them inline:

```java
@POST
public Response ingestEvents(List<TransactionEvent> events) {
    // Validate
    if (events == null || events.isEmpty()) {
        return Response.status(BAD_REQUEST)
                .entity(new ErrorResponse("Events list cannot be null or empty"))
                .build();
    }
    if (events.size() > maxBatchSize) {
        return Response.status(BAD_REQUEST)
                .entity(new ErrorResponse("Batch size exceeds maximum " + maxBatchSize))
                .build();
    }

    // Dispatch each event asynchronously
    DeliveryOptions options = new DeliveryOptions().setLocalOnly(true).setSendTimeout(5000);
    int dispatched = 0;
    for (TransactionEvent event : events) {
        try {
            eventBus.send("transaction.process", event, options);
            dispatched++;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to dispatch event for CIN: %s", event.cin());
        }
    }

    // Return immediately — don't wait for processing
    return Response.accepted()
            .entity(new AcceptedResponse(dispatched, events.size()))
            .build();
}
```

On the consumer side, worker verticles process events on virtual threads:

```java
@ConsumeEvent(value = "transaction.process", blocking = true)
@RunOnVirtualThread
public void processTransaction(TransactionEvent event) {
    List<Rule> rules = ruleCacheService.getCachedRules();
    List<RuleEvaluationResult> results = celRuleEngine.evaluateEvent(event, rules);

    // Single-pass counting
    long matchedCount = 0, errorCount = 0;
    for (RuleEvaluationResult result : results) {
        if (result.hasError()) errorCount++;
        else if (result.matched()) matchedCount++;
    }

    // Alert on matches
    handleMatchedRules(event, results);
}
```

Why Vert.x Event Bus instead of Kafka or RabbitMQ?

- **No external infrastructure** — the event bus is in-process
- **Microsecond latency** — no network hop
- **Backpressure built-in** — send timeout prevents flooding
- **Perfect for single-node** — if you need distributed processing, swap in Kafka later

---

## Immutable Data Models with Java Records

All data models are Java records — immutable by default, with compact validation:

```java
public record TransactionEvent(
        String debitAccount,
        String creditAccount,
        String cin,
        BigDecimal amount,
        Instant transactedTime
) {
    public TransactionEvent {
        if (debitAccount == null || debitAccount.isBlank())
            throw new IllegalArgumentException("debitAccount cannot be null or blank");
        if (creditAccount == null || creditAccount.isBlank())
            throw new IllegalArgumentException("creditAccount cannot be null or blank");
        if (cin == null || cin.isBlank())
            throw new IllegalArgumentException("cin cannot be null or blank");
        if (amount == null)
            throw new IllegalArgumentException("amount cannot be null");
        if (transactedTime == null)
            throw new IllegalArgumentException("transactedTime cannot be null");
    }
}
```

The `Rule` model includes description and active flag matching the database schema:

```java
public record Rule(
        Long id,
        String expression,
        String description,
        boolean active
) {
    public Rule {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (expression == null || expression.isBlank())
            throw new IllegalArgumentException("expression cannot be null or blank");
    }

    // Convenience constructor
    public Rule(Long id, String expression) {
        this(id, expression, null, true);
    }
}
```

And the evaluation result uses factory methods to enforce correct construction:

```java
public record RuleEvaluationResult(Long ruleId, String expression, boolean matched, String error) {
    public static RuleEvaluationResult success(Long ruleId, String expression, boolean matched) {
        return new RuleEvaluationResult(ruleId, expression, matched, null);
    }
    public static RuleEvaluationResult failure(Long ruleId, String expression, String error) {
        return new RuleEvaluationResult(ruleId, expression, false, error);
    }
    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}
```

---

## Production Hardening

Several features make this production-ready beyond the happy path:

### Configurable Batch Limits

```properties
app.events.max-batch-size=${EVENTS_MAX_BATCH:1000}
```

Prevents a single request from flooding the event bus with millions of events.

### Exception Mapping

```java
@ServerExceptionMapper
public RestResponse<ErrorResponse> mapJsonProcessingException(JsonProcessingException e) {
    return RestResponse.status(BAD_REQUEST,
            new ErrorResponse("BAD_REQUEST", "Malformed JSON in request body"));
}
```

Handles malformed JSON, missing fields, and validation errors with clear 400 responses instead of 500s.

### Health Check with Refresh Tracking

```java
@Readiness
public class RuleCacheHealthCheck implements HealthCheck {
    public HealthCheckResponse call() {
        int cachedRules = ruleCacheService.getCachedRules().size();
        int compiledRules = celRuleEngine.getCachedRuleCount();

        var builder = HealthCheckResponse.named("rule-cache")
                .withData("cachedRules", cachedRules)
                .withData("compiledRules", compiledRules)
                .withData("lastRefreshSucceeded", ruleCacheService.isLastRefreshSucceeded());

        if (cachedRules > 0 && compiledRules > 0) return builder.up().build();

        return builder.down()
                .withData("reason", cachedRules == 0
                        ? "No rules loaded"
                        : "Rules loaded but none compiled successfully")
                .build();
    }
}
```

The health check differentiates between "no rules loaded" (source issue) and "loaded but not compiled" (CEL syntax issue) — critical for debugging in production.

### Partial Compilation Resilience

If one rule has invalid CEL syntax, the others still compile and evaluate. The bad rule is logged and skipped:

```
ERROR: Failed to compile rule 11: bad expression !!! - Error: Syntax error
INFO: Cached 2/3 compiled CEL programs
```

---

## Testing: 129 Tests for ~1000 Lines of Code

The test suite covers every component with edge cases:

| Test Class | Tests | What It Covers |
|---|---|---|
| TransactionEventTest | 23 | Null/blank/empty validation, special chars, equality |
| RuleTest | 12 | Validation, constructors, active flag |
| RuleEvaluationResultTest | 6 | Factory methods, hasError() edge cases |
| CelRuleEngineTest | 38 | All 8 rules with boundary values, cache lifecycle, thread safety |
| TransactionEventCodecTest | 11 | Encode/decode round-trip, precision, special chars |
| TransactionEventResourceTest | 11 | Batch ingestion, validation, error handling |
| RuleManagementResourceTest | 6 | List, stats, refresh endpoints |
| RuleCacheServiceTest | 7 | Startup loading, refresh tracking, immutability |
| RuleRepositoryTest | 10 | Active filtering, unique IDs, idempotent reads |
| RuleCacheHealthCheckTest | 4 | Readiness, liveness, health data |

A few interesting test patterns:

**Boundary value testing on CEL rules:**
```java
@Test
void exactlyAtThresholdDoesNotMatchGreaterThan() {
    var results = celRuleEngine.evaluateEvent(
            event("ACC-001", "ACC-002", "CIN-123", "10000.00"), testRules);
    assertFalse(results.get(0).matched(), "amount > 10000 should not match exactly 10000");
}

@Test
void justAboveThresholdMatches() {
    var results = celRuleEngine.evaluateEvent(
            event("ACC-001", "ACC-002", "CIN-123", "10000.01"), testRules);
    assertTrue(results.get(0).matched());
}
```

**Verifying the cache swap is atomic:**
```java
@Test
void recompilingReplacesOldCache() {
    celRuleEngine.compileAndCacheRules(testRules);
    assertEquals(8, celRuleEngine.getCachedRuleCount());

    celRuleEngine.compileAndCacheRules(List.of(new Rule(100L, "amount > 1.0")));
    assertEquals(1, celRuleEngine.getCachedRuleCount());
}
```

**Testing that results are truly immutable:**
```java
@Test
void resultListIsUnmodifiable() {
    var results = celRuleEngine.evaluateEvent(event(...), testRules);
    assertThrows(UnsupportedOperationException.class,
            () -> results.add(RuleEvaluationResult.success(99L, "x", true)));
}
```

---

## Getting Started

```bash
git clone https://github.com/duttgirish/edios.git
cd edios
./mvnw quarkus:dev
```

Send a test transaction:

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '[{
    "debitAccount": "ACC-001",
    "creditAccount": "ACC-002",
    "cin": "CIN-12345",
    "amount": 15000.00,
    "transactedTime": "2024-01-15T10:30:00Z"
  }]'
```

Check which rules matched in the logs:

```
ALERT: Rule 1 matched for transaction - CIN: CIN-12345, Debit: ACC-001, Credit: ACC-002, Amount: 15000.00
```

View cached rules and stats:

```bash
curl http://localhost:8080/rules/stats
```

---

## What's Next

The current implementation uses in-memory sample rules. The roadmap includes:

- **Database integration** — MariaDB reactive client for rule persistence
- **Metrics** — Micrometer integration for Prometheus/Grafana monitoring
- **Alert actions** — Webhook/Kafka dispatch for matched rules
- **Rule versioning** — Track rule changes over time
- **Native image** — GraalVM compilation for serverless deployment

---

## Wrapping Up

EDIOS demonstrates that you can build a high-throughput, production-grade event processing system with modern Java and relatively little code (~1000 lines of business logic). The key patterns:

1. **Decouple ingestion from processing** — return 202, process async
2. **Pre-compile rules** — pay the cost once, evaluate millions of times
3. **Volatile swap for caches** — no locks, no contention, always consistent
4. **Immutable data models** — thread safety by construction, not convention
5. **Virtual threads** — scale blocking operations without tuning thread pools

The full source is on GitHub: **[github.com/duttgirish/edios](https://github.com/duttgirish/edios)**

Feel free to open issues or contribute. I'd love to hear how you'd extend this for your use case.

---

*Built by [Girish Dutt](https://github.com/duttgirish)*
