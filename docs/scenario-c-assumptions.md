# Scenario C — Compliance Reporting Assumptions

## 1. Ambiguous requirement

The request asks for compliance reporting, but it does not specify the exact regulatory framework, required report fields, reporting format, retention rules, or access-control requirements.

## 2. Assumptions

- A compliance report summarizes audit events recorded by the existing audit log service.
- Reports support a required start and end timestamp, with optional actor ID and event type filters.
- The start and end timestamps are inclusive.
- Reports include total matching events, counts grouped by event type and actor, and the number of archived events.
- The report includes the integrity-verification result for the complete audit chain.
- Reports are generated on demand and are not persisted.
- Timestamps are supplied as ISO-8601 instants, for example `2026-10-01T00:00:00Z`.

## 3. Scope

- Generate an on-demand compliance summary.
- Filter events by time range, actor ID, and event type.
- Return event counts and archived-event counts.
- Include the existing audit-chain verification result.
- Validate reporting parameters.
- Provide automated tests for filtering, aggregation, invalid ranges, and integrity status.

## 4. Exclusions

- No claim of compliance with a specific regulation, such as PCI DSS, GDPR, or SOX.
- No PDF, CSV, or signed report generation.
- No scheduled reporting or report persistence.
- No role-based access control implementation in this scenario.
- No modification or deletion of audit events by the reporting API.
- No guarantee that the report is legally sufficient for an audit.

## 5. Design

The compliance report controller accepts query parameters and delegates to the compliance report service. The service retrieves audit events from the existing repository, applies the requested filters, calculates aggregate counts, and calls the existing verification service.

The report is read-only. It does not change event payloads, hashes, archive status, or the chain checkpoint.

## 6. Integrity limitation

The report's integrity indicator reflects the result of the application's current hash-chain verification. It does not independently prove that the chain checkpoint or database has not been maliciously replaced. Stronger assurance would require a separately protected checkpoint or external immutable storage.

## 7. Acceptance criteria

- Valid reporting requests return the correct counts.
- Actor and event-type filters are applied correctly.
- Invalid time ranges return a client error.
- The response includes the current chain verification status.
- Reporting does not mutate stored audit events.
