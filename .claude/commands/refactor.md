---
description: Refactor specified code for clarity, performance, or a named pattern without changing behaviour. Usage: /refactor [file] [goal?]
---

Refactor `$ARGUMENTS`. Read the file first, understand its tests, then apply the narrowest change that achieves the goal. Do not add features.

## Refactor Goals (detect from argument or infer)

| Keyword | Action |
|---------|--------|
| `extract method` | Pull inline block into named method |
| `extract class` | Split large class by responsibility |
| `flatten` | Remove nested conditionals with early returns |
| `reactive` | Convert blocking code to Mutiny/Reactor pipeline |
| `records` | Replace mutable POJO with Java record (Java 16+) |
| `virtual threads` | Replace thread pool with virtual threads (Java 21+) |
| `streams` | Replace for-loop with Stream pipeline |
| `spark` | Optimise DataFrame operations (remove UDFs, add broadcast) |
| `readability` | Rename + restructure for clarity without logic change |
| `performance` | Apply performance patterns appropriate to language/framework |

## Rules (always apply)
1. **Tests must pass before and after** — run existing tests; do not weaken assertions
2. **Behaviour unchanged** — if behaviour must change, stop and ask
3. **One concern per commit** — do not mix rename + logic change
4. **No new features** — if you notice missing functionality, note it as `TODO:` but do not implement
5. **Preserve public API** — do not rename public methods or change signatures without explicit request

## Java Refactor Patterns

### Replace nested null checks (flatten)
```java
// Before
if (order != null) {
    if (order.getCustomer() != null) {
        return order.getCustomer().getEmail();
    }
}
return null;

// After
return Optional.ofNullable(order)
    .map(Order::getCustomer)
    .map(Customer::getEmail)
    .orElse(null);
```

### Convert to record
```java
// Before: mutable POJO with getters/setters
// After
public record TransactionEvent(String cin, String debitAccount, double amount) {}
```

### Flatten reactive chain
```java
// Before: nested subscriptions
repo.findById(id).subscribe(order -> {
    enricher.enrich(order).subscribe(enriched -> { ... });
});

// After: flat chain
repo.findById(id)
    .flatMap(enricher::enrich)
    .subscribe(...);
```

## Python Refactor Patterns

### Replace UDF with SQL function
```python
# Before
udf_fn = udf(lambda x: x.upper(), StringType())
df.withColumn("name", udf_fn(col("name")))

# After (10x faster)
df.withColumn("name", upper(col("name")))
```

### Replace nested conditionals
```python
# Before: deeply nested if/else
# After: early returns + match/case (Python 3.10+)
def classify(amount: float) -> str:
    if amount <= 0:
        return "INVALID"
    if amount > 50_000:
        return "CRITICAL"
    if amount > 10_000:
        return "HIGH"
    return "NORMAL"
```

## Output Format
Show a unified diff-style view (before → after) for each changed block.
Summarise in one sentence what changed and why (the design reason, not the mechanical change).
List any tests that cover the refactored code so reviewer can verify.
