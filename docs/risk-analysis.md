
# Audit Log Service — Risk Analysis

| Risk | Impact | Mitigation | Validation |
|---|---|---|---|
| Event content is modified | Audit evidence may be unreliable | Recalculate event hashes and verify the chain | Tamper-detection test |
| Checkpoint is replaced | A rebuilt chain may appear valid | Protect checkpoints independently | Threat-model and recovery review |
| Concurrent writes | Duplicate sequences or chain forks | Transactional writes, locking, and database constraints | Concurrent creation test |
| Sensitive data in payloads | Personal or credential exposure | Minimize payloads and redact sensitive fields | Nested redaction tests |
| Unredacted export exposure | Bulk data leakage | Restrict export access and audit export operations | Security integration tests |
| Incorrect retention cutoff | Events archived too early or too late | Define cutoff semantics and test boundaries | Retention tests |
| Archive metadata modified | Archive status may be untrustworthy | Protect or audit metadata changes | Integrity-model review |
| In-memory database | Data lost after restart | Use persistent storage for durable deployments | Restart and persistence test |
| Missing authorization | Unauthorized access or operations | Authentication, role-based authorization, least privilege | Security tests |
| Large exports | Excessive memory or response time | Query limits, pagination, streaming, or asynchronous exports | Load testing |
| Hash algorithm changes | Existing events may fail verification | Version canonicalization and preserve verification compatibility | Cross-version tests |
| Mock-only testing | Database or HTTP failures may be missed | Add integration tests against the real application | Full integration suite |

## Risk assessment

This project is a prototype. It must not be described as absolutely tamper-proof
or production-ready without additional controls and validation.

## Residual risks

A hash chain detects certain modifications but does not prevent all database
tampering. Export redaction does not replace access control or safe data
collection. A compliance report does not prove regulatory compliance.

## Required follow-up

Prioritize persistent storage, authorization, independent checkpoint protection,
transaction and concurrency testing, and operational recovery before production use.

### Filtered Export and Verification Metadata Disclosure

**Risk:** Export verification metadata can reveal information about events that are not included in the main filtered export.

**Current mitigation:** Verification metadata contains sequence numbers, timestamps, previous hashes and event hashes, but not the original payloads. The main export applies actor/resource filtering and optional payload redaction.

**Residual risk:** Chain metadata can still reveal event timing and chain structure. The metadata does not independently prove the inclusion of filtered events against a trusted checkpoint.

**Required follow-up:** Define and implement a recipient-verifiable proof format anchored to a checkpoint whose authenticity is established independently of the export. Test that proof verification rejects altered exported records and altered checkpoints. Consider whether metadata for unrelated events can be omitted without weakening the proof.
