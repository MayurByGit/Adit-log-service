# Audit Log Service — API Design

## Overview

Local base URL: `http://localhost:8080`

The Audit Log Service exposes APIs for creating audit events, querying records, verifying hash-chain integrity, applying retention, exporting records, and generating compliance reports.

## 1. Create an Event

**Endpoint:** `POST /audit/events`

**Content-Type:** `application/json`

Example request:

```json
{
  "eventType": "USER_CREATED",
  "actorId": "user-123",
  "resourceType": "USER",
  "resourceId": "user-001",
  "payload": {
    "status": "ACTIVE"
  }
}
```

The service assigns the sequence number, timestamp, previous hash, and event hash. The event is persisted and the chain checkpoint is updated within a transaction. The endpoint returns HTTP 201 Created on successful creation. The API does not expose event update or delete operations.

## 2. Query Events

**Endpoint:** `GET /audit/events`

The endpoint supports the following optional filters:

- `actorId`
- `resourceType`
- `resourceId`
- `eventType`
- `from`
- `to`

Results are ordered by `sequenceNumber` ascending. Pagination uses zero-based `page` and `size` parameters. The default page is `0`, the default page size is `20`, and the maximum page size is `100`.

The `from` and `to` parameters use ISO-8601 date-time values. If both are supplied, `from` must not be later than `to`. Negative page values, page sizes below 1, and page sizes above 100 are rejected.

Verify the actual HTTP status and error response for invalid input before documenting exact error codes.

## 3. Verify Chain Integrity

**Endpoint:** `GET /audit/verify`

Checks sequence numbers, previous hashes, event hashes, and the chain checkpoint.

The response includes:

- `valid`: whether verification succeeded
- `firstInvalidSequence`: first inconsistency, if detected
- `violationType`: the type of violation, if detected
- `message`: explanation of the result

Example response for a valid chain:

```json
{
  "valid": true,
  "firstInvalidSequence": null,
  "violationType": null,
  "message": "Audit chain is valid"
}
```

The exact response message depends on the implementation.

## 4. Retention

**Endpoint:** `POST /audit/archive/retention?retentionDays=365`

Marks events older than the configured retention cutoff as archived. Archiving does not necessarily move records to a separate physical archive.

Invalid retention values should return a client error. Confirm the actual status code and error response by testing the endpoint.

Archived records should remain verifiable according to the chain design.

## 5. Audit Export

**Endpoint:** `GET /audit/archive/export`

**Query parameters:**

- `redact` — optional boolean; defaults to `false`.
- `actorId` — optional actor filter.
- `resourceId` — optional resource filter.

The actor and resource filters can be combined. The main `events` list contains only events matching the supplied filters. When redaction is enabled, recognized sensitive fields are masked in the exported payloads.

The response includes server-side integrity status and chain-link metadata. The metadata deliberately excludes original event payloads to avoid disclosing unrelated raw event content.

**Verification limitation:** The current metadata does not, by itself, provide a complete independent proof that every filtered event belongs to a trusted chain. A chain-head hash contained in the same export is not an independent trust anchor. Do not treat the export as a complete cryptographic inclusion proof until a trusted checkpoint and recipient-side verification procedure have been implemented and tested.

**Privacy limitation:** Chain metadata for events outside the main filtered result is still included. This can disclose event timing and chain structure. Evaluate whether this metadata is appropriate for the intended recipient and use case.


## 6. Compliance Report

**Endpoint:** `GET /audit/compliance/report`

Example:

`GET /audit/compliance/report?startTime=2026-10-01T00:00:00Z&endTime=2026-10-31T23:59:59Z`

Optional filters:

- `actorId`
- `eventType`

Returns the total number of matching events, counts by event type and actor, archived-event counts, and the current chain-verification result.

The time range and filter behavior should match the implementation.

## 7. Immutability

The public API does not expose an event update or delete endpoint.

The append-only API design does not by itself prevent direct database modifications. Hash-chain verification is used to detect inconsistencies, subject to the security of the stored checkpoint and underlying database.

## 8. Errors

Document the actual status codes and response bodies after testing.

Expected validation cases include:

- Invalid or missing event fields
- Invalid time range
- Invalid retention values
- Unsupported query parameters, if rejected
- Internal server errors

Do not document an error status as guaranteed until confirmed against the controller and exception handling.

## 9. Security and Limitations

Document whether authentication and authorization are implemented.

Do not claim production-grade access controls if the prototype does not enforce them.

Other limitations to disclose include the database configuration, export verification approach, redaction scope, retention behavior, and any unimplemented requirements.
