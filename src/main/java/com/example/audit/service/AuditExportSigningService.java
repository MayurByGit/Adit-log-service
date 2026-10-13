package com.example.audit.service;

import com.example.audit.dto.SignedAuditExportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuditExportSigningService {

    private static final String ALGORITHM = "Ed25519";
    private static final String PAYLOAD_PREFIX = "AUDIT-EXPORT-V1\n";

    private final KeyPair keyPair;
    private final String keyId;

    public AuditExportSigningService(
            @Value("${audit.export.signing.private-key:}") String privateKey,
            @Value("${audit.export.signing.public-key:}") String publicKey) {
        this.keyPair = loadOrGenerate(privateKey, publicKey);
        validateKeyPair(this.keyPair);
        this.keyId = fingerprint(keyPair.getPublic().getEncoded());
    }

    public SignedAuditExportResponse sign(byte[] payload) {
        try {
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(keyPair.getPrivate());
            signer.update(PAYLOAD_PREFIX.getBytes(StandardCharsets.UTF_8));
            signer.update(keyId.getBytes(StandardCharsets.US_ASCII));
            signer.update((byte) '\n');
            signer.update(payload);

            return new SignedAuditExportResponse(
                    "1",
                    "base64url(UTF-8 JSON bytes)",
                    Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(payload),
                    ALGORITHM,
                    keyId,
                    Base64.getEncoder().encodeToString(
                            keyPair.getPublic().getEncoded()),
                    Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(signer.sign())
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to sign audit export", exception);
        }
    }

    private KeyPair loadOrGenerate(String privateKeyBase64,
                                  String publicKeyBase64) {
        boolean hasPrivate = privateKeyBase64 != null
                && !privateKeyBase64.isBlank();
        boolean hasPublic = publicKeyBase64 != null
                && !publicKeyBase64.isBlank();

        if (hasPrivate != hasPublic) {
            throw new IllegalStateException(
                    "Configure both audit export signing keys, or neither");
        }

        try {
            if (hasPrivate) {
                KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
                PrivateKey privateKey = keyFactory.generatePrivate(
                        new PKCS8EncodedKeySpec(
                                Base64.getDecoder().decode(privateKeyBase64)));
                PublicKey publicKey = keyFactory.generatePublic(
                        new X509EncodedKeySpec(
                                Base64.getDecoder().decode(publicKeyBase64)));
                return new KeyPair(publicKey, privateKey);
            }

            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Invalid audit export signing key configuration",
                    exception);
        }
    }

    private String fingerprint(byte[] encodedPublicKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(encodedPublicKey);
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate signing key fingerprint", exception);
        }
    }

    private void validateKeyPair(KeyPair pair) {
        try {
            byte[] challenge = "audit-export-key-check"
                    .getBytes(StandardCharsets.UTF_8);
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(pair.getPrivate());
            signer.update(challenge);
            byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(pair.getPublic());
            verifier.update(challenge);
            if (!verifier.verify(signature)) {
                throw new IllegalStateException(
                        "Audit export signing keys do not match");
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to validate audit export signing keys",
                    exception);
        }
    }
}
