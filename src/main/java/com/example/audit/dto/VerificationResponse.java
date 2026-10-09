package com.example.audit.dto;

public record VerificationResponse(
        boolean valid,
        Long firstInvalidSequence,
        String violationType,
        String message
) {
}
