---
description: Design or document REST/event API contracts from a feature description or existing code. Usage: /api-design [feature-or-file]
---

Design or document the API for `$ARGUMENTS`. If given a feature description, produce a contract. If given an existing endpoint file, extract and document the contract.

## Output: REST Contract

```
### [HTTP METHOD] /[resource-path]

**Purpose**: [one sentence — business action performed]
**Auth**: [None | Bearer JWT | API Key | mTLS]
**Idempotent**: [Yes / No — reason]

#### Request
Content-Type: application/json

Path params:
  {id}  String  required  [description]

Query params:
  page  Integer  optional  default=0  [pagination offset]
  size  Integer  optional  default=20  max=100

Body:
  {
    "fieldName": [type, required/optional, constraints, example]
  }

#### Responses
200 OK
  {
    "fieldName": [type, description]
  }

400 Bad Request — [when: validation failed]
  { "error": "VALIDATION_FAILED", "details": [...] }

404 Not Found — [when: resource does not exist]
409 Conflict   — [when: duplicate / state conflict]
500 Internal   — [when: unexpected — include correlation ID]

#### Business Rules Applied
- BR-[ID]: [rule name]

#### Events Published
- [topic/address]: [event schema summary] — [when published]
```

## Output: Event Bus / Message Contract

```
### Event: [event.name]

**Published by**: [service/verticle]
**Consumed by**: [service/verticle list]
**Trigger**: [business action that causes this event]

Payload schema:
  {
    "field": [type, nullable, description]
  }

Guarantees:
  Delivery  : [at-most-once | at-least-once | exactly-once]
  Ordering  : [per-partition key | unordered]
  Retention : [duration or count]

Error handling:
  [What consumer does on processing failure]
```

## Design Rules
1. Use nouns for resources, not verbs: `/orders` not `/getOrders`
2. Collections: plural (`/orders`); single resource: `/orders/{id}`
3. Sub-resources for tight ownership: `/orders/{id}/items`
4. Avoid deep nesting (> 2 levels) — use query params instead
5. Return `202 Accepted` for async operations; include a polling/webhook mechanism
6. Pagination: cursor-based for large/real-time datasets; offset for admin UIs
7. Versioning: path prefix (`/v1/`) for breaking changes; headers for content negotiation
8. Error envelope: always `{ "error": "CODE", "message": "human text", "correlationId": "..." }`

## When Designing from Feature Description
1. Identify actors from the description — each actor may need different endpoints
2. Identify commands (state changes) → `POST`/`PUT`/`PATCH`/`DELETE`
3. Identify queries (reads) → `GET`
4. Identify events (side effects) → event bus messages
5. Surface ambiguities as `OPEN:` items before finalising
