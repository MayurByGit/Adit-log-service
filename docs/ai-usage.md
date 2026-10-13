
# AI Usage Record

## Purpose

This document records AI assistance used while developing the Audit Log Service.

## Assistance received

AI assistance was used to:
- Discuss the layered architecture and API contracts.
- Draft and revise Java implementation examples.
- Develop examples for hash-chain verification, retention, export, redaction,
  and compliance reporting.
- Draft automated test examples and troubleshoot reported test failures.
- Prepare architecture documentation, API documentation, risk analysis,
  engineering summaries, and submission checklists.

## Developer responsibilities

The developer is responsible for:
- Reviewing and adapting generated code to the actual project.
- Understanding the final implementation and design decisions.
- Running the test suite and checking actual results.
- Validating endpoint behavior and documentation accuracy.
- Reviewing security limitations and avoiding unsupported claims.
- Checking that no secrets or real sensitive data are committed.

## Actual validation record

- **Additional validation commands:** `mvnw.cmd -DskipTests test` and `mvnw.cmd -Dtest=AuditExportSigningServiceTest test` against this checkout; the full suite was also attempted but did not complete during Mockito dynamic JVM attachment
- **Execution date:** 2026-10-13
- **Result:** Main and test sources compiled successfully with tests skipped. The targeted signature test passed (1/1). The full suite did not complete and no aggregate pass total is claimed for the updated code.
- **Endpoints tested in this work session:** None.
- **Code and documentation reviewed:** `VerificationService.java`, `AuditEvent.java`, `AuditScenarioATest.java`, `api-design.md`, `engineering-summary.md`, `README.md`, and this file.
- **AI-generated or AI-assisted changes:** Improved missing-sequence reporting, added integration cases for deleted events and payload canonicalization, removed the unused redacted-payload field, implemented signed export envelopes and signature tests, and updated the API, architecture, risk, scenario, and validation documentation.
- **Human review:** The developer should review these changes and independently rerun the tests before submission.
- **Rejected or modified suggestions:** The export is signed to detect changes after issue, but a Merkle inclusion proof was not added. Export redaction remains a view-level transformation; it does not erase stored values. The API documentation records both limitations.

This record describes the actual development process. It does not claim that AI-generated code was automatically correct or that untested behavior works.

## Accuracy statement

This record must reflect the assistance actually used and the work
actually performed. It does not claim that all generated suggestions
are correct, that all tests passed, or that every feature is complete.
