---
description: Review staged or specified code for correctness, security, performance, and style. Usage: /review [file-or-diff]
---

Review the code provided (or `git diff --staged` if no argument given). Use the developer agent matching the file language/framework.

## Review Dimensions (in priority order)

### 1. Correctness
- Logic errors, off-by-one, null dereference, race conditions
- Missing error handling at system boundaries
- Incorrect async/reactive usage (blocking calls on event loop)
- SQL injection, command injection, unvalidated inputs

### 2. Security
- OWASP Top 10 violations
- Secrets hardcoded or logged
- Overly permissive access control
- Sensitive data exposed in responses or logs

### 3. Performance
- N+1 queries or missing pagination
- Unbounded collections / missing limits
- Unnecessary object allocation in hot paths
- Missing indexes implied by query patterns
- Blocking I/O on reactive threads

### 4. Design
- Single Responsibility violations (class/method doing too many things)
- Missing abstraction or premature abstraction
- Tight coupling that prevents testing
- Breaking existing contracts (API, event schema)

### 5. Style
- Naming clarity
- Dead code or commented-out blocks
- Missing or incorrect type annotations (Python/Java)

## Output Format

```
## Summary
[2–3 sentences: overall quality and main concern]

## Critical (must fix before merge)
- [FILE:LINE] [issue] — [why it matters] — [suggested fix]

## Major (should fix)
- [FILE:LINE] [issue] — [suggested fix]

## Minor (consider)
- [FILE:LINE] [issue]

## Positive
- [What was done well — specific, not generic]
```

Rules:
- Every finding must cite file and line number
- Every Critical/Major item must include a concrete fix, not just diagnosis
- If no issues found in a category, omit the section
- Do not pad with generic praise
