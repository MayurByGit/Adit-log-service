
        package com.example.audit.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class HashChainService {

    private static final String GENESIS_VALUE =
            "AUDIT_LOG_GENESIS_V1";

    public String calculateHash(
            Long sequenceNumber,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payload,
            Instant timestamp,
            String previousHash) {

        if (sequenceNumber == null || timestamp == null) {
            throw new IllegalArgumentException(
                    "Sequence number and timestamp are required");
        }

        // Use microsecond precision consistently.
        Instant normalizedTimestamp =
                timestamp.truncatedTo(ChronoUnit.MICROS);

        StringBuilder content = new StringBuilder();

        appendField(content, sequenceNumber.toString());
        appendField(content, eventType);
        appendField(content, actorId);
        appendField(content, resourceType);
        appendField(content, resourceId);
        appendField(content, payload);
        appendField(content, normalizedTimestamp.toString());
        appendField(content, previousHash);

        return sha256(content.toString());
    }

    public String getGenesisHash() {
        return sha256(GENESIS_VALUE);
    }

    private void appendField(StringBuilder target, String value) {
        if (value == null) {
            target.append("-1:");
        } else {
            target.append(value.length())
                    .append(':')
                    .append(value);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", exception);
        }
    }
}