
package com.example.audit.service;

import com.example.audit.dto.ComplianceReportResponse;
import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ComplianceReportService {

    private final AuditEventRepository auditEventRepository;
    private final VerificationService verificationService;

    public ComplianceReportService(
            AuditEventRepository auditEventRepository,
            VerificationService verificationService) {

        this.auditEventRepository = auditEventRepository;
        this.verificationService = verificationService;
    }

    @Transactional(readOnly = true)
    public ComplianceReportResponse generateReport(
            Instant startTime,
            Instant endTime,
            String actorId,
            String eventType) {

        validateTimeRange(startTime, endTime);

        String actorFilter = normalize(actorId);
        String typeFilter = normalize(eventType);

        List<AuditEvent> events =
                auditEventRepository
                        .findAllByOrderBySequenceNumberAsc()
                        .stream()
                        .filter(event -> {
                            Instant timestamp = event.getTimestamp();
                            return timestamp != null
                                    && !timestamp.isBefore(startTime)
                                    && !timestamp.isAfter(endTime);
                        })
                        .filter(event ->
                                actorFilter == null
                                        || actorFilter.equals(
                                        event.getActorId()))
                        .filter(event ->
                                typeFilter == null
                                        || (event.getEventType() != null
                                        && typeFilter.equalsIgnoreCase(
                                        event.getEventType())))
                        .toList();

        Map<String, Long> eventTypeCounts =
                events.stream()
                        .collect(Collectors.groupingBy(
                                AuditEvent::getEventType,
                                TreeMap::new,
                                Collectors.counting()));

        Map<String, Long> actorCounts =
                events.stream()
                        .collect(Collectors.groupingBy(
                                AuditEvent::getActorId,
                                TreeMap::new,
                                Collectors.counting()));

        long archivedCount = events.stream()
                .filter(AuditEvent::isArchived)
                .count();

        VerificationResponse verification =
                verificationService.verify();

        return new ComplianceReportResponse(
                Instant.now(),
                startTime,
                endTime,
                actorFilter,
                typeFilter,
                events.size(),
                archivedCount,
                eventTypeCounts,
                actorCounts,
                verification.valid(),
                verification.message()
        );
    }

    private void validateTimeRange(
            Instant startTime,
            Instant endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "startTime and endTime are required");
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "startTime must be before or equal to endTime");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
