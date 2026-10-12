
# Audit Log Service — Architecture

## 1. Purpose

The Audit Log Service is a Spring Boot REST application for recording,
querying, verifying, archiving, exporting, and reporting audit events.

## 2. Technology stack

- Java 21
- Spring Boot
- Spring Data JPA
- H2 for local development and testing
- Maven
- SHA-256 for event hashing
- JUnit and Mockito for automated tests

Confirm the versions against pom.xml before submission.

## 3. Layered architecture

### Controller layer
Receives HTTP requests, validates input, and returns responses.

### Service layer
Implements audit-event creation, hash-chain verification, archive and
export operations, redaction, and compliance reporting.

### Repository layer
Uses Spring Data JPA to read and persist audit events and chain state.

### Entity layer
Represents audit events and the chain checkpoint in the database.

### DTO layer
Separates HTTP request and response structures from persistence entities.

## 4. Event creation flow

1. The client sends an event creation request.
2. The service acquires the chain-state lock.
3. It obtains the next sequence number and previous hash.
4. It normalizes the timestamp and serializes the payload.
5. It calculates the event hash.
6. It persists the event and updates the checkpoint in one transaction.
7. The API returns the saved event response.

## 5. Hash-chain verification

The first event references a configured genesis hash. Each subsequent
event references the previous event's hash.

Verification checks:
- Sequence continuity.
- Previous-hash linkage.
- Recalculated event hashes.
- Final chain checkpoint consistency.

The hash-chain algorithm must use a stable canonical representation.
Changing the algorithm requires a migration and versioning strategy.

## 6. Retention and export

Retention marks eligible events as archived without deleting their
original event records. Export returns event data and the current
integrity-verification result.

When redaction is requested, sensitive payload fields are masked in
the exported representation. The stored payload should remain unchanged.

Verify these behaviors against the final implementation.

## 7. Compliance reporting

The compliance report summarizes events over a requested time range,
with optional actor and event-type filters. It includes grouped counts,
archive counts, and chain-verification status.

The report is not a certification of compliance with a specific regulation.

## 8. Persistence

The local development environment uses H2. If the configured JDBC URL
uses an in-memory database, data may be lost when the application stops.

Production deployment would require persistent storage, migrations,
backups, monitoring, authentication, authorization, and secure
secret management.

## 9. Security and limitations

A hash chain can detect certain changes to stored event content, but it
does not prevent an administrator from replacing the database and
recomputing the chain.

The checkpoint should be protected independently for stronger assurance.
Audit payloads should minimize sensitive information, and unredacted
exports should be access-controlled.

## 10. Known limitations

Document the limitations actually observed in the implementation,
including concurrency testing, storage durability, authentication,
checkpoint protection, and scalability.
