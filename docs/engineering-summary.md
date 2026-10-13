
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
- Generate a scoped compliance summary for a time range.

## Design decisions

- Separate API DTOs from persistence entities.
- Perform event and checkpoint writes in a transaction.
- Keep the reporting endpoint read-only.
- Redact a copy of the payload rather than altering stored event content.
- Report integrity-verification status explicitly.
- Document limitations instead of claiming absolute tamper-proofing.

## Validation record

- **Prior recorded run:** The README records `mvn clean test` on 2026-10-12 with 19 passing tests. That result predates the changes below and was not independently checked in this work session.
- **Date checked:** 2026-10-13
- **Commands attempted:** `mvnw.cmd test` and `mvnw.cmd -DforkCount=0 test` against the extracted reference copy; `mvnw.cmd -DskipTests test` against this checkout.
- **Compilation:** Main and test sources in this checkout compiled successfully with tests skipped.
- **Test result:** The test process in the extracted reference copy did not complete during Spring test startup in this environment; no Surefire result report was produced. No test totals are claimed for that run.
- **Application startup result:** The Spring context started as part of the test process, but the overall test run did not complete.
- **Endpoints verified manually:** None during this work session.
- **Known unverified paths:** Full test suite and manual endpoint behavior remain to be run and confirmed.

The build result and automated test results do not independently establish the correctness of every API or the recipient-side verification of exported bundles.

## Known limitations

- Record the actual database configuration, security controls, concurrency
behavior, checkpoint protection, and report scalability.

- An in-memory database is not durable across restarts. Hash-chain verification
does not independently protect a checkpoint stored in the same database.

- H2 is configured as an in-memory database, so records are not durable across application restarts.
- Export-time redaction changes the exported payload, not the original payload stored in the database.
- The exported chain metadata is not independently trusted merely because it is included in the export.
- The prototype does not claim production-grade authentication or authorization unless those controls are implemented and tested.
## Conclusion

The project implements the core audit-event workflow, querying, hash-chain verification, retention marking, export-time redaction, and compliance reporting.

The current export contains chain metadata and server-side integrity status. Independent verification by a recipient against a trusted checkpoint remains a separate requirement to implement and validate.

The project is a prototype. Production deployment would require durable storage, appropriate authentication and authorization, independent checkpoint protection, concurrency and recovery testing, and validation of the export-verification design.
