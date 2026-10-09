
package com.example.audit.dto;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

public record AuditEventResponse(
        Long id,
        Long sequenceNumber,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        Instant timestamp
) {}
