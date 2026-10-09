
package com.example.audit.service;

import com.example.audit.dto.VerificationResponse;
import com.example.audit.entity.AuditChainState;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditChainStateRepository;
import com.example.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VerificationService {

    private static final Long CHAIN_STATE_ID = 1L;

    private final AuditEventRepository eventRepository;
    private final AuditChainStateRepository stateRepository;
    private final HashChainService hashChainService;

    public VerificationService(
            AuditEventRepository eventRepository,
            AuditChainStateRepository stateRepository,
            HashChainService hashChainService) {
        this.eventRepository = eventRepository;
        this.stateRepository = stateRepository;
        this.hashChainService = hashChainService;
    }

    @Transactional(readOnly = true)
    public VerificationResponse verify() {
        List<AuditEvent> events =
                eventRepository.findAllByOrderBySequenceNumberAsc();

        AuditChainState checkpoint =
                stateRepository.findById(CHAIN_STATE_ID).orElse(null);

        if (events.isEmpty()) {
            if (checkpoint != null
                    && (checkpoint.getLatestSequence() != 0L
                    || !hashChainService.getGenesisHash()
                    .equals(checkpoint.getLatestHash()))) {
                return invalid(
                        null,
                        "CHECKPOINT_MISMATCH",
                        "Checkpoint is inconsistent with an empty chain");
            }

            return new VerificationResponse(
                    true, null, null, "Audit chain is intact");
        }

        long expectedSequence = 1L;
        String expectedPreviousHash = hashChainService.getGenesisHash();

        for (AuditEvent event : events) {
            Long actualSequence = event.getSequenceNumber();

            if (actualSequence == null
                    || actualSequence != expectedSequence) {
                return invalid(
                        actualSequence,
                        "SEQUENCE_MISMATCH",
                        "Audit event sequence is missing or out of order");
            }

            if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                return invalid(
                        actualSequence,
                        "PREVIOUS_HASH_MISMATCH",
                        "Event previous hash does not match the preceding hash");
            }

            String calculatedHash = hashChainService.calculateHash(
                    event.getSequenceNumber(),
                    event.getEventType(),
                    event.getActorId(),
                    event.getResourceType(),
                    event.getResourceId(),
                    event.getPayload(),
                    event.getTimestamp(),
                    event.getPreviousHash());

            if (!calculatedHash.equals(event.getEventHash())) {
                return invalid(
                        actualSequence,
                        "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash");
            }

            expectedPreviousHash = event.getEventHash();
            expectedSequence++;
        }

        AuditEvent lastEvent = events.get(events.size() - 1);

        if (checkpoint == null
                || checkpoint.getLatestSequence() == null
                || checkpoint.getLatestSequence()
                != lastEvent.getSequenceNumber()
                || !lastEvent.getEventHash()
                .equals(checkpoint.getLatestHash())) {
            return invalid(
                    lastEvent.getSequenceNumber(),
                    "CHECKPOINT_MISMATCH",
                    "Chain checkpoint does not match the last event");
        }

        return new VerificationResponse(
                true, null, null, "Audit chain is intact");
    }

    private VerificationResponse invalid(
            Long sequence,
            String violationType,
            String message) {
        return new VerificationResponse(
                false, sequence, violationType, message);
    }
}
