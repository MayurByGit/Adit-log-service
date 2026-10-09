
package com.example.audit.controller;

import com.example.audit.dto.AuditEventResponse;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit/events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventResponse create(
            @Valid @RequestBody CreateAuditEventRequest request) {
        return auditService.create(request);
    }
}
