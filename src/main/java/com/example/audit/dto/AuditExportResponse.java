
package com.example.audit.dto;

import java.time.Instant;
import java.util.List;

public record AuditExportResponse(
        Instant exportedAt,
        int totalEvents,
        int archivedEvents,
        boolean integrityVerified,
        String integrityMessage,
        List<AuditEventResponse> events,
        AuditExportVerificationData verificationData
) {
}
