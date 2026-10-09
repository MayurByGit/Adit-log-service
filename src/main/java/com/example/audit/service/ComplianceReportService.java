package com.example.audit.service;

import com.example.audit.dto.ComplianceReportResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import com.example.audit.dto.VerificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        List<AuditEvent> matchingEvents =
                auditEventRepository.findAllByOrderBySequenceNumberAsc()
                        .stream()
                        .filter(event ->
                                !event.getTimestamp().isBefore(startTime)
                                        && !event.getTimestamp().isAfter(endTime))
                        .filter(event ->
                                actorId == null
                                        || actorId.isBlank()
                                        || actorId.equals(event.getActorId()))
                        .filter(event ->
                                eventType == null
                                        || eventType.isBlank()
                                        || eventType.equalsIgnoreCase(
                                        event.getEventType()))
                        .toList();

        Map<String, Long> eventTypeCounts =
                matchingEvents.stream()
                        .collect(Collectors.groupingBy(
                                AuditEvent::getEventType,
                                TreeMap::new,
                                Collectors.counting()));

        Map<String, Long> actorCounts =
                matchingEvents.stream()
                        .collect(Collectors.groupingBy(
                                AuditEvent::getActorId,
                                TreeMap::new,
                                Collectors.counting()));

        long archivedEvents = matchingEvents.stream()
                .filter(AuditEvent::isArchived)
                .count();

        VerificationResponse verification = verificationService.verify();

        List<ComplianceReportResponse.EventSummary> summaries =
                matchingEvents.stream()
                        .map(this::toSummary)
                        .toList();

        return new ComplianceReportResponse(
                Instant.now(),
                startTime,
                endTime,
                normalizeFilter(actorId),
                normalizeFilter(eventType),
                matchingEvents.size(),
                archivedEvents,
                eventTypeCounts,
                actorCounts,
                verification.valid(),
                verification.message(),
                summaries
        );
    }

    private void validateTimeRange(Instant startTime, Instant endTime) {
        Objects.requireNonNull(startTime, "startTime is required");
        Objects.requireNonNull(endTime, "endTime is required");

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "startTime must be before or equal to endTime");
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private ComplianceReportResponse.EventSummary toSummary(
            AuditEvent event) {
        return new ComplianceReportResponse.EventSummary(
                event.getSequenceNumber(),
                event.getEventType(),
                event.getActorId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getTimestamp(),
                event.isArchived()
        );
    }
}