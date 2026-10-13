
# Audit Log Service — Architecture

## 1. Purpose

The Audit Log Service is a Spring Boot REST application for recording,
querying, verifying, archiving, exporting, and reporting audit events.

## 2. Technology Stack

- Java 17
- Spring Boot 4.1.1
- Spring Data JPA
- H2 database for local development and testing
- Maven
- SHA-256 for event hashing
- JUnit and the test libraries declared in `pom.xml`

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

Payload object keys are sorted recursively in Java string order before JSON
serialization. Array order and scalar values are preserved. The resulting
compact JSON string is stored and hashed. The hash input then length-prefixes
sequence number, event type, actor ID, resource type, resource ID, canonical
payload JSON, microsecond-normalized timestamp, and previous hash, in that
order, using UTF-8 and SHA-256.

## 6. Retention and export

Retention marks eligible events as archived without deleting their
original event records. Export returns event data and the current
integrity-verification result.

When redaction is requested, sensitive payload fields are masked in the
exported representation. The stored payload remains unchanged. Export data is
serialized once, then signed with Ed25519; the envelope includes the exact
signed bytes, public key, key fingerprint, and signature so a recipient can
detect changes to the bundle after export. A recipient must pin the key
fingerprint out of band to authenticate the signer. Local development uses an
ephemeral key unless a stable key pair is configured through environment
variables.

The signature does not provide a Merkle inclusion proof or allow the recipient
to recompute omitted records whose payloads are excluded from a filtered
export. The signed verification status is the service's chain check at export
time.

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

The configured H2 database is in-memory and loses records across restarts.
Authentication and authorization are not implemented. The checkpoint is stored
in the same database as the events. Concurrency beyond the database lock and
report/export scalability have not been load-tested. Export redaction does not
erase stored values. Signed exports do not provide inclusion proofs for omitted
records and require an out-of-band pinned key fingerprint to authenticate the
signer.
