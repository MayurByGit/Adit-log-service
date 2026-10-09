package com.example.audit.service;
import com.example.audit.dto.AuditEventResponse;
import com.example.audit.dto.AuditExportResponse;
import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
public class AuditArchiveService {

    private final AuditEventRepository repository;
    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;

    public AuditArchiveService(
            AuditEventRepository repository,
            VerificationService verificationService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int archiveOlderThanDays(int retentionDays) {
        if (retentionDays < 1 || retentionDays > 36500) {
            throw new IllegalArgumentException(
                    "retentionDays must be between 1 and 36500");
        }

        Instant cutoff = Instant.now()
                .minus(retentionDays, ChronoUnit.DAYS);

        List<AuditEvent> expiredEvents =
                repository.findByTimestampBeforeAndArchivedFalse(cutoff);

        Instant archivedAt = Instant.now();

        for (AuditEvent event : expiredEvents) {
            event.markArchived(archivedAt);
        }

        repository.saveAll(expiredEvents);
        return expiredEvents.size();
    }

    @Transactional(readOnly = true)
    public AuditExportResponse exportEvents(boolean redactSensitiveData) {
        VerificationResponse verification = verificationService.verify();

        List<AuditEvent> allEvents =
                repository.findAllByOrderBySequenceNumberAsc();

        List<AuditEventResponse> exportedEvents = new ArrayList<>();
        int archivedCount = 0;

        for (AuditEvent event : allEvents) {
            if (event.isArchived()) {
                archivedCount++;
            }

            JsonNode payload = parsePayload(event.getPayload());

            if (redactSensitiveData) {
                payload = redactPayload(payload);
            }

            exportedEvents.add(new AuditEventResponse(
                    event.getId(),
                    event.getSequenceNumber(),
                    event.getEventType(),
                    event.getActorId(),
                    event.getResourceType(),
                    event.getResourceId(),
                    payload,
                    event.getTimestamp(),
                    event.getPreviousHash(),
                    event.getEventHash()
            ));
        }

        return new AuditExportResponse(
                Instant.now(),
                exportedEvents.size(),
                archivedCount,
                verification.valid(),
                verification.message(),
                exportedEvents
        );
    }

    public JsonNode redactPayload(JsonNode original) {
        if (original == null) {
            return null;
        }

        JsonNode copy = original.deepCopy();
        redactRecursively(copy);
        return copy;
    }

    private void redactRecursively(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof ObjectNode objectNode) {

            // Jackson 3: propertyNames() returns a Set<String>
            List<String> fieldNames =
                    new ArrayList<>(objectNode.propertyNames());

            for (String fieldName : fieldNames) {
                JsonNode value = objectNode.get(fieldName);

                if (isSensitiveField(fieldName)) {
                    objectNode.put(fieldName, "[REDACTED]");
                } else {
                    redactRecursively(value);
                }
            }

        } else if (node.isArray()) {
            for (JsonNode child : node) {
                redactRecursively(child);
            }
        }
    }





    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase(Locale.ROOT);

        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("ssn")
                || normalized.contains("socialsecurity")
                || normalized.contains("creditcard")
                || normalized.contains("accountnumber");
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not parse audit event payload",
                    exception);
        }
    }
}
