
package com.example.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateAuditEventRequest(

        @NotBlank
        @Size(max = 100)
        String eventType,

        @NotBlank
        @Size(max = 150)
        String actorId,

        @NotBlank
        @Size(max = 100)
        String resourceType,

        @NotBlank
        @Size(max = 150)
        String resourceId,

        @NotNull
        JsonNode payload

) {}
