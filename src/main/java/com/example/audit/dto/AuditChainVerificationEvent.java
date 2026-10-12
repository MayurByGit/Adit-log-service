
package com.example.audit.dto;

import java.time.Instant;

/**
 * Original, unredacted event fields needed to recalculate
 * the event hash independently.
 *
 * WARNING: This contains the original payload and can expose
 * sensitive data. Treat the verification bundle as sensitive.
 */
public record AuditChainVerificationEvent(
        Long sequenceNumber,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        String payloadJson,
        Instant timestamp,
        String previousHash,
        String eventHash
) {
}
