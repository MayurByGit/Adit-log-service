package com.example.audit.service;

import com.example.audit.dto.SignedAuditExportResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditExportSigningServiceTest {

    @Test
    void recipientCanVerifyExportAndDetectPayloadChanges() throws Exception {
        AuditExportSigningService signingService =
                new AuditExportSigningService("", "");
        byte[] originalPayload = "{\"totalEvents\":1}"
                .getBytes(StandardCharsets.UTF_8);

        SignedAuditExportResponse envelope =
                signingService.sign(originalPayload);
        byte[] decodedPayload = Base64.getUrlDecoder()
                .decode(envelope.signedPayload());
        byte[] signatureBytes = Base64.getUrlDecoder()
                .decode(envelope.signature());
        byte[] publicKeyBytes = Base64.getDecoder()
                .decode(envelope.publicKey());

        var publicKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        assertEquals("1", envelope.formatVersion());
        assertEquals("Ed25519", envelope.signatureAlgorithm());
        assertEquals(
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(publicKeyBytes)),
                envelope.signingKeyId());
        assertTrue(verify(envelope, publicKey, decodedPayload, signatureBytes));

        decodedPayload[decodedPayload.length - 2] = '2';
        assertFalse(verify(envelope, publicKey, decodedPayload, signatureBytes));
    }

    private boolean verify(
            SignedAuditExportResponse envelope,
            java.security.PublicKey publicKey,
            byte[] payload,
            byte[] signatureBytes) throws Exception {
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update("AUDIT-EXPORT-V1\n"
                .getBytes(StandardCharsets.UTF_8));
        verifier.update(envelope.signingKeyId()
                .getBytes(StandardCharsets.US_ASCII));
        verifier.update((byte) '\n');
        verifier.update(payload);
        return verifier.verify(signatureBytes);
    }
}
