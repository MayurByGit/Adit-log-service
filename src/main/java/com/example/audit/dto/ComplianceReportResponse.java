
package com.example.audit.dto;

import java.time.Instant;
import java.util.Map;

public record ComplianceReportResponse(
        Instant generatedAt,
        Instant startTime,
        Instant endTime,
        String actorId,
        String eventType,
        long totalEvents,
        long archivedEvents,
        Map<String, Long> eventTypeCounts,
        Map<String, Long> actorCounts,
        boolean integrityVerified,
        String integrityMessage
) {
}
