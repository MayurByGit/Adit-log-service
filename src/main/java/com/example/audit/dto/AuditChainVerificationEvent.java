
package com.example.audit.dto;

import java.time.Instant;

/**
 * Metadata for an event in the audit hash chain.
 *
 * The payload is deliberately excluded so that a filtered export
 * does not disclose unrelated original event payloads.
 *
 * This metadata alone does not prove the integrity of an omitted
 * event's content. See the export verification documentation.
 */
public record AuditChainVerificationEvent(
        Long sequenceNumber,
        String previousHash,
        String eventHash,
        Instant timestamp
) {
}
