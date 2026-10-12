
package com.example.audit.dto;

import java.util.List;

/**
 * Data required to independently recalculate the exported chain.
 *
 * The chain head must be compared against a trusted value
 * obtained independently of this export.
 */
public record AuditExportVerificationData(
        String hashAlgorithm,
        String canonicalization,
        String genesisHash,
        Long chainHeadSequence,
        String chainHeadHash,
        List<AuditChainVerificationEvent> verificationEvents
) {
}
