
        package com.example.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;


@Entity
@Table(
        name = "audit_event",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "sequence_number")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number", nullable = false, unique = true)
    private Long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor_id", nullable = false, length = 150)
    private String actorId;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 150)
    private String resourceId;

    @Column(name = "payload", nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(name = "event_timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "redacted_payload", columnDefinition = "CLOB")
    private String redactedPayload;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    // Required by Hibernate
    protected AuditEvent() {
    }

    public AuditEvent(
            Long sequenceNumber,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payload,
            Instant timestamp,
            String previousHash,
            String eventHash) {

        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.actorId = actorId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payload = payload;
        this.timestamp = timestamp;
        this.previousHash = previousHash;
        this.eventHash = eventHash;
    }

    public Long getId() {
        return id;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getEventHash() {
        return eventHash;
    }
    public String getRedactedPayload() {
        return redactedPayload;
    }



    public void setRedactedPayload(String redactedPayload) {
        this.redactedPayload = redactedPayload;
    }

    public void markArchived() {
        this.archived = true;
    }
    public boolean isArchived() {
        return archived;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void markArchived(Instant archivedAt) {
        this.archived = true;
        this.archivedAt = archivedAt;
    }
}
