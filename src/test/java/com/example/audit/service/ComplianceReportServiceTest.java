package com.example.audit.service;

import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceReportServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private VerificationService verificationService;

    private ComplianceReportService service;

    @BeforeEach
    void setUp() {
        service = new ComplianceReportService(
                auditEventRepository, verificationService);
    }

    @Test
    void shouldGenerateReportWithCorrectCounts() {
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-31T23:59:59Z");

        AuditEvent first = mockEvent(
                1L, "USER_CREATED", "user-123",
                "2026-10-09T12:00:00Z", false);

        AuditEvent second = mockEvent(
                2L, "PAYMENT_CREATED", "user-123",
                "2026-10-09T12:30:00Z", true);

        AuditEvent outsideRange = mockEvent(
                3L, "USER_DELETED", "user-456",
                "2026-11-09T12:00:00Z", false);

        when(auditEventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(first, second, outsideRange));

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        true, null, null, "Audit chain is intact"));

        var report = service.generateReport(start, end, null, null);

        assertEquals(2, report.totalEvents());
        assertEquals(1, report.archivedEvents());
        assertEquals(1L, report.eventTypeCounts().get("USER_CREATED"));
        assertEquals(1L, report.eventTypeCounts().get("PAYMENT_CREATED"));
        assertEquals(2L, report.actorCounts().get("user-123"));
        assertTrue(report.integrityVerified());
        assertEquals(2, report.events().size());

        verify(verificationService).verify();
    }

    @Test
    void shouldFilterByActorAndEventType() {
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-31T23:59:59Z");

        AuditEvent matching = mockEvent(
                1L, "USER_CREATED", "user-123",
                "2026-10-09T12:00:00Z", false);

        AuditEvent differentActor = mockEvent(
                2L, "USER_CREATED", "user-456",
                "2026-10-09T12:30:00Z", false);

        AuditEvent differentType = mockEvent(
                3L, "PAYMENT_CREATED", "user-123",
                "2026-10-09T13:00:00Z", false);

        when(auditEventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(matching, differentActor, differentType));

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        true, null, null, "Audit chain is intact"));

        var report = service.generateReport(
                start, end, "user-123", "USER_CREATED");

        assertEquals(1, report.totalEvents());
        assertEquals("user-123", report.actorId());
        assertEquals("USER_CREATED", report.eventType());
        assertEquals(1, report.events().size());
        assertEquals(1L, report.eventTypeCounts().get("USER_CREATED"));
    }

    @Test
    void shouldReturnIntegrityFailureInReport() {
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-31T23:59:59Z");

        when(auditEventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of());

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        false, 1L, "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash"));

        var report = service.generateReport(start, end, null, null);

        assertFalse(report.integrityVerified());
        assertEquals(
                "Stored event content does not match its hash",
                report.integrityMessage());
    }

    @Test
    void shouldRejectStartTimeAfterEndTime() {
        Instant start = Instant.parse("2026-10-10T00:00:00Z");
        Instant end = Instant.parse("2026-10-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> service.generateReport(start, end, null, null));

        verifyNoInteractions(auditEventRepository, verificationService);
    }

    private AuditEvent mockEvent(
            Long sequence,
            String eventType,
            String actorId,
            String timestamp,
            boolean archived) {

        AuditEvent event = mock(AuditEvent.class);
        when(event.getSequenceNumber()).thenReturn(sequence);
        when(event.getEventType()).thenReturn(eventType);
        when(event.getActorId()).thenReturn(actorId);
        when(event.getResourceType()).thenReturn("TEST_RESOURCE");
        when(event.getResourceId()).thenReturn("resource-" + sequence);
        when(event.getTimestamp()).thenReturn(Instant.parse(timestamp));
        when(event.isArchived()).thenReturn(archived);

        return event;
    }
}