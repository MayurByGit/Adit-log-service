
# Audit Log Service — Engineering Summary

## Objective

Build a REST-based audit logging prototype that supports event creation,
querying, hash-chain verification, retention, export, and compliance reporting.

## Architecture

The application uses Java and Spring Boot with controller, service, repository,
entity, and DTO layers. Spring Data JPA provides persistence, and H2 is used
for local development and tests.

## Core functionality

- Create audit events with sequence numbers, timestamps, previous hashes,
  and event hashes.
- Query events with supported filters and pagination.
- Verify sequence continuity, hash links, event content, and the checkpoint.
- Archive events according to a configured retention period.
- Export events with optional export-time sensitive-data redaction.
- Sign export bundles with Ed25519 so recipients can detect changes after export.
- Generate a scoped compliance summary for a time range.

## Design decisions

- Separate API DTOs from persistence entities.
- Perform event and checkpoint writes in a transaction.
- Keep the reporting endpoint read-only.
- Redact a copy of the payload rather than altering stored event content.
- Sign the exact serialized export bytes and include the public key fingerprint.
- Canonicalize payload JSON by recursively sorting object keys while preserving array order.
- Report integrity-verification status explicitly.
- Document limitations instead of claiming absolute tamper-proofing.

## Validation record

- **Prior recorded run:** The README records `mvn clean test` on 2026-10-12 with 19 passing tests. That result predates the changes below and was not independently checked in this work session.
- **Date checked:** 2026-10-13
- **Compilation:** `mvnw.cmd -DskipTests test` completed successfully; main and test sources compiled.
- **Targeted test:** `mvnw.cmd -Dtest=AuditExportSigningServiceTest test` passed (1 test).
- **Full suite:** A run did not complete after Mockito attempted dynamic JVM attachment. No aggregate test totals are claimed for the updated code.
- **Current test source count:** 23 `@Test` methods.
- **Endpoints verified manually:** None during this work session.
- **Known unverified paths:** Full test suite and manual endpoint behavior remain to be run and confirmed.

The build result and automated test results do not independently establish the correctness of every API or the recipient-side verification of exported bundles.

## Known limitations

- H2 is configured as an in-memory database, so records are not durable across application restarts.
- The chain checkpoint is stored in the same database and is not independently protected.
- Authentication and authorization are not implemented.
- Concurrency behavior beyond the single-database pessimistic lock and report/export scalability have not been load-tested.
- Export-time redaction changes the exported payload, not the original payload stored in the database.
- Signed filtered exports do not provide Merkle inclusion proofs for omitted records; signing-key identity requires an out-of-band pinned fingerprint.
- The prototype does not claim production-grade authentication or authorization unless those controls are implemented and tested.
## Conclusion

The project implements the core audit-event workflow, querying, hash-chain verification, retention marking, export-time redaction, and compliance reporting.

The signed export lets recipients verify that the bundle bytes have not changed since export. It does not independently prove omitted event contents against an externally trusted checkpoint. Recipients need an out-of-band trusted key fingerprint to authenticate the signer.

The project is a prototype. Production deployment would require durable storage, appropriate authentication and authorization, independent checkpoint protection, concurrency and recovery testing, and validation of the export-verification design.
