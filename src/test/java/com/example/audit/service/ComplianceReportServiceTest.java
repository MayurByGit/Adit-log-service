
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
    private AuditEventRepository eventRepository;

    @Mock
    private VerificationService verificationService;

    private ComplianceReportService service;

    private final Instant start =
            Instant.parse("2026-10-01T00:00:00Z");

    private final Instant end =
            Instant.parse("2026-10-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        service = new ComplianceReportService(
                eventRepository,
                verificationService
        );
    }

    @Test
    void shouldGenerateReportWithCorrectCounts() {

        AuditEvent event1 = createEvent(
                1L, "USER_CREATED", "user-123",
                "2026-10-09T12:00:00Z", false);

        AuditEvent event2 = createEvent(
                2L, "PAYMENT_CREATED", "user-123",
                "2026-10-09T13:00:00Z", true);

        AuditEvent outsideRange = createEvent(
                3L, "USER_DELETED", "user-456",
                "2026-11-09T12:00:00Z", false);

        when(eventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(
                        event1, event2, outsideRange));

        mockValidVerification();

        var report = service.generateReport(
                start, end, null, null);

        assertEquals(2, report.totalEvents());
        assertEquals(1, report.archivedEvents());

        assertEquals(
                1L,
                report.eventTypeCounts().get("USER_CREATED"));

        assertEquals(
                1L,
                report.eventTypeCounts().get("PAYMENT_CREATED"));

        assertEquals(
                2L,
                report.actorCounts().get("user-123"));

        assertTrue(report.integrityVerified());

        verify(verificationService).verify();
    }

    @Test
    void shouldFilterByActorAndEventType() {

        AuditEvent event1 = createEvent(
                1L, "USER_CREATED", "user-123",
                "2026-10-09T12:00:00Z", false);

        AuditEvent event2 = createEvent(
                2L, "USER_CREATED", "user-456",
                "2026-10-09T12:30:00Z", false);

        AuditEvent event3 = createEvent(
                3L, "PAYMENT_CREATED", "user-123",
                "2026-10-09T13:00:00Z", false);

        when(eventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(event1, event2, event3));

        mockValidVerification();

        var report = service.generateReport(
                start,
                end,
                "user-123",
                "USER_CREATED"
        );

        assertEquals(1, report.totalEvents());
        assertEquals("user-123", report.actorId());
        assertEquals("USER_CREATED", report.eventType());

        assertEquals(
                1L,
                report.eventTypeCounts().get("USER_CREATED"));

        assertEquals(
                1L,
                report.actorCounts().get("user-123"));
    }

    @Test
    void shouldIncludeStartAndEndBoundaries() {

        AuditEvent first = createEvent(
                1L, "USER_CREATED", "user-123",
                start.toString(), false);

        AuditEvent last = createEvent(
                2L, "USER_UPDATED", "user-123",
                end.toString(), false);

        when(eventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(first, last));

        mockValidVerification();

        var report = service.generateReport(
                start, end, null, null);

        assertEquals(2, report.totalEvents());
    }

    @Test
    void shouldReturnIntegrityFailure() {

        when(eventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of());

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        false,
                        1L,
                        "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash"
                ));

        var report = service.generateReport(
                start, end, null, null);

        assertFalse(report.integrityVerified());

        assertEquals(
                "Stored event content does not match its hash",
                report.integrityMessage());
    }

    @Test
    void shouldRejectInvalidTimeRange() {

        Instant invalidStart =
                Instant.parse("2026-11-01T00:00:00Z");

        Instant invalidEnd =
                Instant.parse("2026-10-01T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.generateReport(
                        invalidStart,
                        invalidEnd,
                        null,
                        null
                )
        );

        verifyNoInteractions(
                eventRepository,
                verificationService);
    }

    @Test
    void shouldReturnEmptyReportWhenNoEventsMatch() {

        when(eventRepository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of());

        mockValidVerification();

        var report = service.generateReport(
                start, end, null, null);

        assertEquals(0, report.totalEvents());
        assertEquals(0, report.archivedEvents());
        assertTrue(report.eventTypeCounts().isEmpty());
        assertTrue(report.actorCounts().isEmpty());
        assertTrue(report.integrityVerified());
    }

    private void mockValidVerification() {
        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain is intact"
                ));
    }

    private AuditEvent createEvent(
            Long sequence,
            String eventType,
            String actorId,
            String timestamp,
            boolean archived) {

        AuditEvent event = new AuditEvent(
                sequence,
                eventType,
                actorId,
                "TEST_RESOURCE",
                "resource-" + sequence,
                "{\"status\":\"ACTIVE\"}",
                Instant.parse(timestamp),
                "previous-hash",
                "event-hash"
        );

        if (archived) {
            event.markArchived(
                    Instant.parse("2026-10-10T00:00:00Z"));
        }

        return event;
    }
}
