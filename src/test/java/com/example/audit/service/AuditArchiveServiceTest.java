
package com.example.audit.service;

import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditArchiveServiceTest {

    @Mock
    private AuditEventRepository repository;

    @Mock
    private VerificationService verificationService;

    private ObjectMapper objectMapper;
    private AuditArchiveService service;
    HashChainService hashChainService = new HashChainService();


    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AuditArchiveService(
                repository,
                verificationService,
                objectMapper,
                hashChainService);
    }


    @Test
    void shouldRedactSensitiveFieldsRecursively() throws Exception {
        JsonNode original = objectMapper.readTree("""
                {
                  "name": "Test User",
                  "password": "synthetic-password",
                  "profile": {
                    "email": "test@example.com",
                    "status": "ACTIVE"
                  },
                  "sessions": [
                    {
                      "accessToken": "synthetic-token",
                      "label": "test-session"
                    }
                  ]
                }
                """);

        JsonNode redacted = (JsonNode) invokeRedaction(original);

        assertEquals("Test User", redacted.get("name").asString());
        assertEquals("[REDACTED]",
                redacted.get("password").asString());
        assertEquals("[REDACTED]",
                redacted.get("profile").get("email").asString());
        assertEquals("ACTIVE",
                redacted.get("profile").get("status").asString());
        assertEquals("[REDACTED]",
                redacted.get("sessions").get(0)
                        .get("accessToken").asString());

        // Original data must remain unchanged.
        assertEquals("synthetic-password",
                original.get("password").asString());
        assertEquals("test@example.com",
                original.get("profile").get("email").asString());
    }

    @Test
    void shouldExportEventsAndReportIntegrityStatus() {
        AuditEvent event = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                """
                {"email":"test@example.com","status":"ACTIVE"}
                """,
                false);

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(event));

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        true, null, null, "Audit chain is intact"));

        var result = service.exportEvents(true,null,null);

        assertEquals(1, result.totalEvents());
        assertEquals(0, result.archivedEvents());
        assertTrue(result.integrityVerified());
        assertEquals("Audit chain is intact", result.integrityMessage());

        JsonNode exportedPayload = result.events().get(0).payload();
        assertEquals("[REDACTED]",
                exportedPayload.get("email").asString());
        assertEquals("ACTIVE",
                exportedPayload.get("status").asString());

        verify(verificationService).verify();
    }

    @Test
    void shouldReportFailedIntegrityVerificationOnExport() {
        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of());

        when(verificationService.verify())
                .thenReturn(new VerificationResponse(
                        false, 1L, "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash"));

        var result = service.exportEvents(false,null,null);

        assertFalse(result.integrityVerified());
        assertEquals(
                "Stored event content does not match its hash",
                result.integrityMessage());
        assertEquals(0, result.totalEvents());
    }

    @Test
    void shouldArchiveEventsOlderThanRetentionCutoff() {
        AuditEvent oldEvent = mockEvent(
                1L, "USER_CREATED", "user-123",
                "{\"status\":\"ACTIVE\"}", false);

        AuditEvent recentEvent = mockEvent(
                2L, "USER_UPDATED", "user-123",
                "{\"status\":\"ACTIVE\"}", false);

        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)))
                .thenReturn(List.of(oldEvent));

        when(repository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int archived = service.archiveOlderThanDays(365);

        assertEquals(1, archived);
        verify(oldEvent).markArchived(any(Instant.class));
        verify(repository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldReturnZeroWhenThereAreNoEventsToArchive() {
        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)))
                .thenReturn(List.of());

        assertEquals(0, service.archiveOlderThanDays(365));

        verify(repository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void shouldRejectInvalidRetentionDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(0));

        assertThrows(IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(-1));

        assertThrows(IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(36501));

        verifyNoInteractions(repository);
    }

    private AuditEvent mockEvent(
            Long sequence,
            String eventType,
            String actorId,
            String payload,
            boolean archived) {

        AuditEvent event = mock(AuditEvent.class);

        lenient().when(event.getId()).thenReturn(sequence);
        lenient().when(event.getSequenceNumber()).thenReturn(sequence);
        lenient().when(event.getEventType()).thenReturn(eventType);
        lenient().when(event.getActorId()).thenReturn(actorId);
        lenient().when(event.getResourceType()).thenReturn("USER");
        lenient().when(event.getResourceId()).thenReturn("resource-" + sequence);
        lenient().when(event.getPayload()).thenReturn(payload);
        lenient().when(event.getTimestamp())
                .thenReturn(Instant.parse("2026-10-01T00:00:00Z"));
        lenient().when(event.getPreviousHash()).thenReturn("previous-hash");
        lenient().when(event.getEventHash()).thenReturn("event-hash");
        lenient().when(event.isArchived()).thenReturn(archived);

        return event;
    }

    /*
     * Keeps the redaction method private in production.
     * Tests the real export path for most behavior; this helper uses
     * reflection only to isolate the recursive redaction unit.
     */
    private Object invokeRedaction(JsonNode original) throws Exception {
        var method = AuditArchiveService.class
                .getDeclaredMethod("redactPayload", JsonNode.class);
        method.setAccessible(true);
        return method.invoke(service, original);
    }
}
