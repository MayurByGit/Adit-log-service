package com.example.audit.dto;

import java.time.Instant;
import java.util.List;
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
        String integrityMessage,
        List<EventSummary> events
) {
    public record EventSummary(
            Long sequenceNumber,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            Instant timestamp,
            boolean archived
    ) {
    }
}