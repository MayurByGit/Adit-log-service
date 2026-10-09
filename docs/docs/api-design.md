
# Audit Log Service — API Design

Local base URL: http://localhost:8080

## 1. Create an event

POST /audit/events

Content-Type: application/json

Example request:

{
"eventType": "USER_CREATED",
"actorId": "user-123",
"resourceType": "USER",
"resourceId": "user-001",
"payload": {
"status": "ACTIVE"
}
}

The service assigns the event sequence, timestamp, previous hash,
and event hash, then persists the event and updates the checkpoint.

## 2. Query events

GET /audit/events?page=0&size=10

Optional filtering parameters depend on the implemented controller.
Document actorId, eventType, resourceType, resourceId, and time filters
only if they are supported by the actual code.

The response uses pagination metadata if the controller returns a
Spring Data Page.

## 3. Verify integrity

GET /audit/verify

Checks sequence numbers, previous hashes, event hashes, and the chain
checkpoint. The response includes a validity flag and diagnostic details.

## 4. Retention

POST /audit/archive/retention?retentionDays=365

Marks events older than the configured retention cutoff as archived.
It does not necessarily move them to a separate physical archive.

Invalid retention values should return a client error.

## 5. Export

GET /audit/archive/export?redact=true

Returns export metadata, audit events, and integrity-verification status.
When redaction is enabled, recognized sensitive fields are masked in
the exported payload representation.

The unredacted export should not be exposed without appropriate
access controls.

## 6. Compliance report

GET /audit/compliance/report?startTime=2026-10-01T00:00:00Z&endTime=2026-10-31T23:59:59Z

Optional parameters:
- actorId
- eventType

Returns total matching events, counts by event type and actor, archived
event counts, and the current chain-verification result.

## 7. Errors

Document actual status codes and error response bodies after testing.
Expected cases include invalid request fields, invalid time ranges,
invalid retention values, and unexpected internal errors.

## 8. Security

State whether authentication and authorization are implemented.
Do not claim production access controls if the prototype does not enforce them.
