package com.example.audit.dto;

/**
 * A signed envelope. Decode signedPayload using payloadEncoding to obtain
 * the exact JSON bytes covered by the Ed25519 signature.
 */
public record SignedAuditExportResponse(
        String formatVersion,
        String payloadEncoding,
        String signedPayload,
        String signatureAlgorithm,
        String signingKeyId,
        String publicKey,
        String signature
) {
}
