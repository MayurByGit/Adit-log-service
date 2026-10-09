
package com.example.audit.service;

import com.example.audit.dto.AuditEventResponse;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final JsonMapper jsonMapper;

    public AuditService(
            AuditEventRepository repository,
            JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public AuditEventResponse create(CreateAuditEventRequest request) {
        String payloadJson =
                jsonMapper.writeValueAsString(request.payload());

        long sequence = repository.count() + 1;

        AuditEvent event = new AuditEvent(
                sequence,
                request.eventType(),
                request.actorId(),
                request.resourceType(),
                request.resourceId(),
                payloadJson,
                Instant.now()
        );

        AuditEvent saved = repository.saveAndFlush(event);

        JsonNode payload = jsonMapper.readTree(saved.getPayload());

        return new AuditEventResponse(
                saved.getId(),
                saved.getSequenceNumber(),
                saved.getEventType(),
                saved.getActorId(),
                saved.getResourceType(),
                saved.getResourceId(),
                payload,
                saved.getTimestamp()
        );
    }
}
