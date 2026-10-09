package com.example.audit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainState {

    @Id
    private Long id;

    @Column(name = "latest_sequence", nullable = false)
    private Long latestSequence;

    @Column(name = "latest_hash", nullable = false, length = 64)
    private String latestHash;

    protected AuditChainState() {
    }

    public AuditChainState(
            Long id, Long latestSequence, String latestHash) {
        this.id = id;
        this.latestSequence = latestSequence;
        this.latestHash = latestHash;
    }

    public Long getId() {
        return id;
    }

    public Long getLatestSequence() {
        return latestSequence;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void update(Long sequence, String hash) {
        this.latestSequence = sequence;
        this.latestHash = hash;
    }
}