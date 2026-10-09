package com.example.audit.service;

import com.example.audit.dto.AuditExportResponse;
import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditArchiveServiceTest {

    private AuditEventRepository repository;
    private VerificationService verificationService;
    private ObjectMapper objectMapper;
    private AuditArchiveService archiveService;

    @BeforeEach
    void setUp() {
        repository = mock(AuditEventRepository.class);
        verificationService = mock(VerificationService.class);
        objectMapper = new ObjectMapper();

        archiveService = new AuditArchiveService(
                repository,
                verificationService,
                objectMapper
        );
    }

    @Test
    void shouldRedactSensitiveFieldsRecursively() throws Exception {
        JsonNode original = objectMapper.readTree("""
            {
              "name": "Test User",
              "email": "test@example.com",
              "credentials": {
                "password": "secret-value"
              },
              "items": [
                {
                  "access_token": "token-value"
                }
              ]
            }
            """);

        JsonNode redacted = archiveService.redactPayload(original);

        assertEquals(
                "[REDACTED]",
                redacted.get("email").asText()
        );
        assertEquals(
                "[REDACTED]",
                redacted.get("credentials")
                        .get("password").asText()
        );
        assertEquals(
                "[REDACTED]",
                redacted.get("items").get(0)
                        .get("access_token").asText()
        );

        // The source JSON must remain unchanged.
        assertEquals(
                "test@example.com",
                original.get("email").asText()
        );
    }

    @Test
    void shouldExportEventsAndReportVerificationStatus() {
        AuditEvent event = new AuditEvent(
                1L,
                "USER_CREATED",
                "test-user",
                "USER",
                "user-001",
                """
                {"email":"test@example.com","status":"ACTIVE"}
                """,
                Instant.parse("2026-01-01T10:00:00Z"),
                "a".repeat(64),
                "b".repeat(64)
        );

        when(verificationService.verify()).thenReturn(
                new VerificationResponse(
                        true, null, null, "Chain verified"
                )
        );

        when(repository.findAllByOrderBySequenceNumberAsc())
                .thenReturn(List.of(event));

        AuditExportResponse response =
                archiveService.exportEvents(true);

        assertTrue(response.integrityVerified());
        assertEquals(1, response.totalEvents());
        assertEquals(
                "[REDACTED]",
                response.events().get(0)
                        .payload().get("email").asText()
        );
        assertEquals(
                "test@example.com",
                event.getPayload().contains("test@example.com")
                        ? "test@example.com" : null
        );
    }

    @Test
    void shouldArchiveExpiredEvents() {
        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        )).thenReturn(List.of());

        int archivedCount =
                archiveService.archiveOlderThanDays(365);

        assertEquals(0, archivedCount);

        verify(repository).findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        );
    }

    @Test
    void shouldRejectInvalidRetentionPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> archiveService.archiveOlderThanDays(0)
        );

        verifyNoInteractions(repository);
    }
    @Test
    void shouldMarkExpiredEventsAsArchived() {
        AuditEvent oldEvent = new AuditEvent(
                1L,
                "USER_CREATED",
                "test-user",
                "USER",
                "user-001",
                "{\"status\":\"ACTIVE\"}",
                Instant.now().minus(500, java.time.temporal.ChronoUnit.DAYS),
                "a".repeat(64),
                "b".repeat(64)
        );

        when(repository.findByTimestampBeforeAndArchivedFalse(
                any(Instant.class)
        )).thenReturn(List.of(oldEvent));

        int archivedCount =
                archiveService.archiveOlderThanDays(365);

        assertEquals(1, archivedCount);
        assertTrue(oldEvent.isArchived());
        assertNotNull(oldEvent.getArchivedAt());

        verify(repository).saveAll(List.of(oldEvent));
    }
}