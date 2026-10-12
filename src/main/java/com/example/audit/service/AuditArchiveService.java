
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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.example.audit.dto.AuditChainVerificationEvent;
import com.example.audit.dto.AuditExportVerificationData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuditArchiveService {

    private static final int MIN_RETENTION_DAYS = 1;
    private static final int MAX_RETENTION_DAYS = 36500;


    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "passwd",
            "secret",
            "clientsecret",
            "token",
            "accesstoken",
            "refreshtoken",
            "email",
            "phone",
            "phonenumber",
            "ssn",
            "socialsecurity",
            "creditcard",
            "cardnumber",
            "accountnumber"
    );

    private final AuditEventRepository repository;
    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;
    private final HashChainService hashChainService;

    public AuditArchiveService(
            AuditEventRepository repository,
            VerificationService verificationService,
            ObjectMapper objectMapper,
            HashChainService hashChainService) {

        this.repository = repository;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
        this.hashChainService = hashChainService;
    }

    /**
     * Marks unarchived events older than the retention cutoff.
     * Events are retained so their hash-chain history remains available.
     */
    @Transactional
    public int archiveOlderThanDays(int retentionDays) {
        validateRetentionDays(retentionDays);

        Instant cutoff = Instant.now()
                .minus(retentionDays, ChronoUnit.DAYS);

        List<AuditEvent> candidates =
                repository.findByTimestampBeforeAndArchivedFalse(cutoff);

        if (candidates.isEmpty()) {
            return 0;
        }

        Instant archivedAt = Instant.now();

        for (AuditEvent event : candidates) {
            event.markArchived(archivedAt);
        }

        repository.saveAllAndFlush(candidates);

        return candidates.size();
    }


    @Transactional(readOnly = true)
    public AuditExportResponse exportEvents(
            boolean redactSensitiveData,
            String actorId,
            String resourceId) {

        // Load the complete chain in sequence order.
        List<AuditEvent> storedEvents =
                repository.findAllByOrderBySequenceNumberAsc();

        // Verify the complete chain, not just the filtered subset.
        VerificationResponse verification =
                verificationService.verify();

        // Main export: apply actor and resource filters.
        List<AuditEvent> filteredEvents = storedEvents.stream()
                .filter(event ->
                        actorId == null
                                || actorId.isBlank()
                                || actorId.equals(event.getActorId()))
                .filter(event ->
                        resourceId == null
                                || resourceId.isBlank()
                                || resourceId.equals(event.getResourceId()))
                .toList();

        int archivedCount = (int) filteredEvents.stream()
                .filter(AuditEvent::isArchived)
                .count();

        List<AuditEventResponse> exportedEvents =
                filteredEvents.stream()
                        .map(event -> toResponse(
                                event, redactSensitiveData))
                        .toList();

        // Verification data uses the original, unredacted payloads.
        List<AuditChainVerificationEvent> chainEvents =
                storedEvents.stream()
                        .map(event -> new AuditChainVerificationEvent(
                                event.getSequenceNumber(),
                                event.getEventType(),
                                event.getActorId(),
                                event.getResourceType(),
                                event.getResourceId(),
                                event.getPayload(),
                                event.getTimestamp(),
                                event.getPreviousHash(),
                                event.getEventHash()
                        ))
                        .toList();

        AuditEvent lastEvent = storedEvents.isEmpty()
                ? null
                : storedEvents.get(storedEvents.size() - 1);

        AuditExportVerificationData verificationData =
                new AuditExportVerificationData(
                        "SHA-256",
                        "Length-prefixed UTF-16 Java String fields; "
                                + "timestamp truncated to microseconds; "
                                + "fields ordered as sequenceNumber, "
                                + "eventType, actorId, resourceType, "
                                + "resourceId, payloadJson, timestamp, "
                                + "previousHash",
                        hashChainService.getGenesisHash(),
                        lastEvent == null
                                ? 0L
                                : lastEvent.getSequenceNumber(),
                        lastEvent == null
                                ? hashChainService.getGenesisHash()
                                : lastEvent.getEventHash(),
                        chainEvents
                );

        return new AuditExportResponse(
                Instant.now(),
                exportedEvents.size(),
                archivedCount,
                verification.valid(),
                verification.message(),
                exportedEvents,
                verificationData
        );
    }


    private AuditEventResponse toResponse(
            AuditEvent event,
            boolean redactSensitiveData) {

        JsonNode payload = readPayload(event.getPayload());

        if (redactSensitiveData) {
            payload = redactPayload(payload);
        }

        return new AuditEventResponse(
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
        );
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to parse stored audit payload", exception);
        }
    }

    private JsonNode redactPayload(JsonNode original) {
        // Never modify the original payload object.
        JsonNode copy = original.deepCopy();
        redactRecursively(copy);
        return copy;
    }

    private void redactRecursively(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            // Jackson 3 propertyNames() returns a Set, not an Iterator.
            List<String> fieldNames =
                    new ArrayList<>(objectNode.propertyNames());

            for (String fieldName : fieldNames) {
                JsonNode value = objectNode.get(fieldName);

                if (isSensitiveField(fieldName)) {
                    objectNode.put(fieldName, "[REDACTED]");
                } else if (value != null) {
                    redactRecursively(value);
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            for (int index = 0; index < arrayNode.size(); index++) {
                JsonNode child = arrayNode.get(index);
                if (child != null) {
                    redactRecursively(child);
                }
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);

        if (SENSITIVE_FIELDS.contains(normalized)) {
            return true;
        }

        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("socialsecurity")
                || normalized.contains("creditcard")
                || normalized.contains("accountnumber");
    }

    private void validateRetentionDays(int retentionDays) {
        if (retentionDays < MIN_RETENTION_DAYS
                || retentionDays > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException(
                    "retentionDays must be between "
                            + MIN_RETENTION_DAYS + " and "
                            + MAX_RETENTION_DAYS);
        }
    }
}
