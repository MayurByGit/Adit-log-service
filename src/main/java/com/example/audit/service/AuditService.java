
package com.example.audit.service;

import com.example.audit.dto.AuditEventResponse;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.entity.AuditChainState;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditChainStateRepository;
import com.example.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuditService {

    private static final Long CHAIN_STATE_ID = 1L;

    private final AuditEventRepository repository;
    private final AuditChainStateRepository stateRepository;
    private final HashChainService hashChainService;
    private final ObjectMapper objectMapper;

    public AuditService(
            AuditEventRepository repository,
            AuditChainStateRepository stateRepository,
            HashChainService hashChainService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.hashChainService = hashChainService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditEventResponse create(CreateAuditEventRequest request) {
        try {
            AuditChainState state = stateRepository
                    .findByIdForUpdate(CHAIN_STATE_ID)
                    .orElseThrow(() -> new IllegalStateException(
                            "Audit chain state is not initialized"));

            String payloadJson =
                    objectMapper.writeValueAsString(request.payload());

            long sequence = state.getLatestSequence() + 1L;
            String previousHash = state.getLatestHash();

            // Normalize before hashing AND storing.
            Instant timestamp =
                    Instant.now().truncatedTo(ChronoUnit.MICROS);

            String eventHash = hashChainService.calculateHash(
                    sequence,
                    request.eventType(),
                    request.actorId(),
                    request.resourceType(),
                    request.resourceId(),
                    payloadJson,
                    timestamp,
                    previousHash);

            AuditEvent event = new AuditEvent(
                    sequence,
                    request.eventType(),
                    request.actorId(),
                    request.resourceType(),
                    request.resourceId(),
                    payloadJson,
                    timestamp,
                    previousHash,
                    eventHash);

            AuditEvent saved = repository.saveAndFlush(event);

            state.update(sequence, eventHash);
            stateRepository.saveAndFlush(state);

            JsonNode payload =
                    objectMapper.readTree(saved.getPayload());

            return new AuditEventResponse(
                    saved.getId(),
                    saved.getSequenceNumber(),
                    saved.getEventType(),
                    saved.getActorId(),
                    saved.getResourceType(),
                    saved.getResourceId(),
                    payload,
                    saved.getTimestamp(),
                    saved.getPreviousHash(),
                    saved.getEventHash());

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create audit event", ex);
        }
    }
}
