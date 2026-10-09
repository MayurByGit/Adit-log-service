
package com.example.audit.controller;

import com.example.audit.dto.AuditEventResponse;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import com.example.audit.service.AuditService;
import com.example.audit.service.VerificationService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditEventRepository repository;
    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;

    public AuditController(
            AuditService auditService,
            AuditEventRepository repository,
            VerificationService verificationService,
            ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.repository = repository;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventResponse create(
            @Valid @RequestBody CreateAuditEventRequest request) {
        return auditService.create(request);
    }

    @GetMapping("/events")
    public Page<AuditEventResponse> search(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "page must be >= 0 and size must be between 1 and 100");
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must not be after 'to'");
        }

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.ASC, "sequenceNumber"));

        return repository.search(
                actorId, resourceType, resourceId,
                eventType, from, to, pageable
        ).map(this::toResponse);
    }

    @GetMapping("/verify")
    public VerificationResponse verify() {
        return verificationService.verify();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        try {
            JsonNode payload =
                    objectMapper.readTree(event.getPayload());

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
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to read stored audit event", ex);
        }
    }
}
