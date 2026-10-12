
package com.example.audit.dto;

import java.util.List;

/**
 * Verification metadata for an exported audit chain.
 *
 * The chain head in this object is not independently trusted
 * merely because it appears in the export. A recipient must
 * obtain a trusted checkpoint through a separate mechanism.
 *
 * This prototype metadata does not, by itself, prove the
 * inclusion of filtered events in the trusted chain.
 */
public record AuditExportVerificationData(
        String hashAlgorithm,
        String canonicalization,
        String genesisHash,
        Long chainHeadSequence,
        String chainHeadHash,
        List<AuditChainVerificationEvent> chainMetadata
) {
}
