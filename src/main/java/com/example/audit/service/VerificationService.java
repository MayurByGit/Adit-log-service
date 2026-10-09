
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

        AuditChainState state = stateRepository.findById(1L)
                .orElse(null);

        if (state == null) {
            return invalid(
                    null,
                    "CHAIN_STATE_MISSING",
                    "Audit chain state was not found");
        }

        List<AuditEvent> events =
                eventRepository.findAllByOrderBySequenceNumberAsc();

        long expectedSequence = 1L;
        String expectedPreviousHash =
                hashChainService.getGenesisHash();

        for (AuditEvent event : events) {

            if (event.getSequenceNumber() == null
                    || event.getSequenceNumber() != expectedSequence) {
                return invalid(
                        expectedSequence,
                        "SEQUENCE_GAP",
                        "Unexpected audit event sequence");
            }

            if (!expectedPreviousHash.equals(
                    event.getPreviousHash())) {
                return invalid(
                        event.getSequenceNumber(),
                        "PREVIOUS_HASH_MISMATCH",
                        "Previous hash does not match the preceding event");
            }

            String calculatedHash =
                    hashChainService.calculateHash(
                            event.getSequenceNumber(),
                            event.getEventType(),
                            event.getActorId(),
                            event.getResourceType(),
                            event.getResourceId(),
                            event.getPayload(),
                            event.getTimestamp(),
                            event.getPreviousHash()
                    );

            if (!calculatedHash.equals(event.getEventHash())) {
                return invalid(
                        event.getSequenceNumber(),
                        "EVENT_HASH_MISMATCH",
                        "Stored event content does not match its hash");
            }

            expectedPreviousHash = event.getEventHash();
            expectedSequence++;
        }

        long actualLastSequence = expectedSequence - 1;

        if (!Long.valueOf(actualLastSequence).equals(
                state.getLatestSequence())) {
            return invalid(
                    null,
                    "CHECKPOINT_SEQUENCE_MISMATCH",
                    "Chain checkpoint sequence does not match the events");
        }

        if (!expectedPreviousHash.equals(state.getLatestHash())) {
            return invalid(
                    null,
                    "CHECKPOINT_HASH_MISMATCH",
                    "Chain checkpoint hash does not match the events");
        }

        return new VerificationResponse(
                true,
                null,
                null,
                "Audit chain is intact"
        );
    }

    private VerificationResponse invalid(
            Long sequence,
            String violationType,
            String message) {

        return new VerificationResponse(
                false,
                sequence,
                violationType,
                message
        );
    }
}
