
# Audit Log Service — Requirements

## 1. Objective
Build a REST service that stores audit events in an
append-only sequence and detects tampering using a
SHA-256 hash chain.

## 2. Audit Event Fields
- eventType: type of event
- actorId: actor responsible for the event
- resourceType: type of resource affected
- resourceId: identifier of the affected resource
- payload: structured event-specific JSON
- timestamp: server-assigned UTC timestamp

## 3. Functional Requirements
- Append an audit event.
- Query events using optional filters.
- Support pagination.
- Verify the complete hash chain.
- Detect modifications and missing records.
- Do not expose update or delete APIs for events.

## 4. Technical Decisions
- Java 17
- Spring Boot 4.1.1
- Maven
- H2 in-memory database for the prototype
- Spring Data JPA
- SHA-256 for event hashing

## 5. Assumptions
- The server assigns the event timestamp in UTC.
- Records are ordered by a monotonically increasing sequence.
- Event content is serialized deterministically before hashing.
- The first event uses a fixed genesis hash.
- The verification endpoint reports the first detected
  inconsistency.

## 6. Out of Scope for Scenario A
- User authentication and authorization
- Distributed multi-node writes
- External immutable checkpoint storage
- Retention and redaction, which belong to Scenario B
