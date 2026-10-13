# Scenario Execution Plan

This document maps each assignment scenario to its implementation and current
validation evidence. A listed test is evidence of intended coverage; it is not
a claim that the full suite passed after the latest changes.

## Scenario A — Core audit log

### Decomposition

1. Define event fields, timestamp ownership, sequence ordering, genesis value,
   and hash serialization.
2. Implement the event entity, chain checkpoint, repositories, create/query
   APIs, and pagination.
3. Implement full-chain verification for sequence gaps, changed content,
   previous-hash mismatch, and checkpoint mismatch.
4. Validate creation, filtering, pagination, tampering, and missing records.

### Execution

- Event creation assigns an `Instant` timestamp and monotonically increasing
  sequence number. The event and chain checkpoint are written transactionally
  while holding a pessimistic lock on chain state.
- Payload object keys are recursively sorted before serialization; array order
  is preserved. The canonical payload and event fields are hashed with
  SHA-256.
- Query supports the requested actor, resource, event type, and time filters,
  with bounded pagination. The public API has no update or delete operation.
- Verification reports sequence, previous-hash, event-hash, and checkpoint
  inconsistencies. Archived records remain in the verification scan.

### Validation status

- `AuditScenarioATest` covers creation, filters/pagination, event modification,
  deleted middle records, deleted tail records, and canonical payload ordering.
- `mvnw.cmd -DskipTests test` compiled the current main and test sources after
  canonicalization was added. The updated full suite has not completed in this
  environment; run it in the developer's normal Java setup before submission.

## Scenario B — Retention, redaction, and export

### Decomposition

1. Define retention as archiving without deleting chain records.
2. Mask recognized sensitive payload keys in export copies while keeping the
   hash-protected database payload unchanged.
3. Filter exported events by actor and/or resource.
4. Sign the exact export data bytes and document recipient verification and
   trust requirements.
5. Validate retention boundaries, redaction, filtering, signature checks, and
   tampering of signed bytes.

### Execution

- Retention marks old events and preserves them in the chain.
- Redaction is export-time only. Original values remain in the database and
  are still available through normal event queries; this does not implement
  redact-at-rest or erasure.
- Export returns a signed envelope. A recipient decodes `signedPayload`, checks
  the public-key fingerprint, and verifies the Ed25519 signature over the
  documented context and exact payload bytes.
- Stable signing keys can be provided through environment variables. Without
  them, the service creates an ephemeral local-development key at startup.
- Recipients need to pin the expected key fingerprint through a trusted
  out-of-band channel to authenticate the signer.

### Validation status

- `AuditArchiveServiceTest` covers export filtering/redaction and retention
  behavior. `AuditExportSigningServiceTest` passed its signature-validity and
  modified-payload checks in the targeted run.
- A complete chain inclusion proof for omitted event payloads is not provided.
  The signature protects the bundle after export and signs the server's
  verification result; it does not let the recipient recompute the omitted
  records against an external checkpoint.
- The full test suite and recipient verification in another implementation
  remain unverified.

## Scenario C — Compliance reporting

### Decomposition

1. Clarify which access events, time range, filters, groups, and report format
   are required.
2. Record assumptions and scope boundaries before implementation.
3. Implement an on-demand read-only report over the existing audit events.
4. Validate aggregation, filters, invalid ranges, and chain status.

### Execution

- The normalized assumptions and exclusions are documented in
  `scenario-c-assumptions.md`.
- `GET /audit/compliance/report` supports inclusive time bounds and optional
  actor and event-type filters. It returns total and grouped counts, archived
  counts, and the chain-verification status.
- The report is explicitly not a certification of compliance with a named
  regulation.

### Validation status

- `ComplianceReportServiceTest` covers report aggregation, filtering, invalid
  ranges, integrity status, and read-only behavior.
- The full suite has not completed after the latest changes, so rerun and
  record its final result before submission.
