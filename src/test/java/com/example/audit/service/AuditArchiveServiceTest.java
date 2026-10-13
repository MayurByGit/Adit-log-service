package com.example.audit.service;

import com.example.audit.dto.VerificationResponse;
import com.example.audit.dto.SignedAuditExportResponse;
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
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditArchiveServiceTest {

    @Mock
    private AuditEventRepository repository;

    @Mock
    private VerificationService verificationService;

    private ObjectMapper objectMapper;
    private AuditArchiveService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        HashChainService hashChainService = new HashChainService();

        service = new AuditArchiveService(
                repository,
                verificationService,
                objectMapper,
                hashChainService,
                new AuditExportSigningService("", "")
        );
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
        assertEquals("[REDACTED]", redacted.get("password").asString());
        assertEquals(
                "[REDACTED]",
                redacted.get("profile").get("email").asString()
        );
        assertEquals(
                "ACTIVE",
                redacted.get("profile").get("status").asString()
        );
        assertEquals(
                "[REDACTED]",
                redacted.get("sessions").get(0).get("accessToken").asString()
        );

        // Ensure the original JSON was not modified.
        assertEquals(
                "synthetic-password",
                original.get("password").asString()
        );
        assertEquals(
                "test@example.com",
                original.get("profile").get("email").asString()
        );
    }

    @Test
    void shouldExportEventsAndReportIntegrityStatus() throws Exception {
        AuditEvent event = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                "{\"email\":\"test@example.com\",\"status\":\"ACTIVE\"}",
                false
        );

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(event));

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain is intact"
                )
        );

        var result = unpack(service.exportEvents(true, null, null));

        assertEquals(1, result.get("totalEvents").asInt());
        assertEquals(0, result.get("archivedEvents").asInt());
        assertTrue(result.get("integrityVerified").asBoolean());
        assertEquals(
                "Audit chain is intact",
                result.get("integrityMessage").asString()
        );

        JsonNode exportedPayload = result.get("events").get(0).get("payload");

        assertEquals(
                "[REDACTED]",
                exportedPayload.get("email").asString()
        );
        assertEquals(
                "ACTIVE",
                exportedPayload.get("status").asString()
        );

        verify(verificationService).verify();
    }

    @Test
    void shouldReportFailedIntegrityVerificationOnExport() throws Exception {
        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of());

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        false,
                        1L,
                        "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash"
                )
        );

        var result = unpack(service.exportEvents(false, null, null));

        assertFalse(result.get("integrityVerified").asBoolean());
        assertEquals(
                "Stored event content does not match its hash",
                result.get("integrityMessage").asString()
        );
        assertEquals(0, result.get("totalEvents").asInt());

        verify(verificationService).verify();
    }

    @Test
    void shouldArchiveEventsOlderThanRetentionCutoff() {
        AuditEvent oldEvent = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                "{\"status\":\"ACTIVE\"}",
                false
        );

        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        )).thenReturn(List.of(oldEvent));

        when(repository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int archived = service.archiveOlderThanDays(365);

        assertEquals(1, archived);

        verify(repository).findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        );
        verify(oldEvent).markArchived(any(Instant.class));
        verify(repository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldReturnZeroWhenThereAreNoEventsToArchive() {
        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        )).thenReturn(List.of());

        int archived = service.archiveOlderThanDays(365);

        assertEquals(0, archived);

        verify(repository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void shouldRejectInvalidRetentionDays() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(-1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.archiveOlderThanDays(36501)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFilterExportAndExcludeRawPayloadFromChainMetadata()
            throws Exception {

        AuditEvent matchingEvent = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                "{\"email\":\"synthetic@example.com\"}",
                false
        );

        AuditEvent unrelatedEvent = mockEvent(
                2L,
                "USER_UPDATED",
                "other-user",
                "{\"accountNumber\":\"synthetic-account\"}",
                false
        );

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(matchingEvent, unrelatedEvent));

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain is intact"
                )
        );

        var result = unpack(service.exportEvents(true, "user-123", null));

        // Only the matching event should be in the filtered export.
        assertEquals(1, result.get("events").size());
        assertEquals("user-123",
                result.get("events").get(0).get("actorId").asString());
        assertEquals(
                "[REDACTED]",
                result.get("events").get(0).get("payload")
                        .get("email").asString()
        );

        // Verification metadata should cover both stored events.
        assertEquals(
                2,
                result.get("verificationData").get("chainMetadata").size()
        );

        assertEquals(
                1L,
                result.get("verificationData").get("chainMetadata")
                        .get(0).get("sequenceNumber").asLong()
        );
        assertEquals(
                2L,
                result.get("verificationData").get("chainMetadata")
                        .get(1).get("sequenceNumber").asLong()
        );

        // Raw sensitive values must not appear anywhere in the export JSON.
        String exportedJson = objectMapper.writeValueAsString(result);

        assertFalse(exportedJson.contains("synthetic-account"));
        assertFalse(exportedJson.contains("synthetic@example.com"));

        verify(verificationService).verify();
    }

    @Test
    void shouldFilterExportByResourceId() throws Exception {
        AuditEvent matchingEvent = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                "{\"status\":\"ACTIVE\"}",
                false
        );

        AuditEvent unrelatedEvent = mockEvent(
                2L,
                "USER_UPDATED",
                "user-123",
                "{\"status\":\"INACTIVE\"}",
                false
        );

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(matchingEvent, unrelatedEvent));

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain is intact"
                )
        );

        var result = unpack(service.exportEvents(false, null, "resource-1"));

        assertEquals(1, result.get("events").size());
        assertEquals(
                "resource-1",
                result.get("events").get(0).get("resourceId").asString()
        );
        assertEquals(1, result.get("totalEvents").asInt());

        verify(verificationService).verify();
    }

    @Test
    void shouldApplyActorAndResourceFiltersTogether() throws Exception {
        AuditEvent matchingEvent = mockEvent(
                1L,
                "USER_CREATED",
                "user-123",
                "{\"status\":\"ACTIVE\"}",
                false
        );

        AuditEvent wrongActor = mockEvent(
                2L,
                "USER_CREATED",
                "other-user",
                "{\"status\":\"ACTIVE\"}",
                false
        );

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(matchingEvent, wrongActor));

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        true,
                        null,
                        null,
                        "Audit chain is intact"
                )
        );

        var result = unpack(service.exportEvents(
                false,
                "user-123",
                "resource-1"
        ));

        assertEquals(1, result.get("events").size());
        assertEquals("user-123",
                result.get("events").get(0).get("actorId").asString());
        assertEquals(
                "resource-1",
                result.get("events").get(0).get("resourceId").asString()
        );

        verify(verificationService).verify();
    }

    private AuditEvent mockEvent(
            Long sequence,
            String eventType,
            String actorId,
            String payload,
            boolean archived
    ) {
        AuditEvent event = mock(AuditEvent.class);

        lenient().when(event.getId()).thenReturn(sequence);
        lenient().when(event.getSequenceNumber()).thenReturn(sequence);
        lenient().when(event.getEventType()).thenReturn(eventType);
        lenient().when(event.getActorId()).thenReturn(actorId);
        lenient().when(event.getResourceType()).thenReturn("USER");
        lenient().when(event.getResourceId())
                .thenReturn("resource-" + sequence);
        lenient().when(event.getPayload()).thenReturn(payload);
        lenient().when(event.getTimestamp()).thenReturn(
                Instant.parse("2026-10-01T00:00:00Z")
        );
        lenient().when(event.getPreviousHash()).thenReturn("previous-hash");
        lenient().when(event.getEventHash()).thenReturn("event-hash");
        lenient().when(event.isArchived()).thenReturn(archived);

        return event;
    }

    private Object invokeRedaction(JsonNode original) throws Exception {
        var method = AuditArchiveService.class.getDeclaredMethod(
                "redactPayload",
                JsonNode.class
        );

        method.setAccessible(true);
        return method.invoke(service, original);
    }

    private JsonNode unpack(SignedAuditExportResponse envelope) {
        byte[] json = Base64.getUrlDecoder()
                .decode(envelope.signedPayload());
        return objectMapper.readTree(json);
    }
}
