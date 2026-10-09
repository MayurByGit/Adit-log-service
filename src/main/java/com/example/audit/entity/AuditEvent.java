
package com.example.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "audit_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_audit_event_sequence",
                        columnNames = "sequence_number"
                )
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number", nullable = false)
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

    protected AuditEvent() {
    }

    public AuditEvent(
            Long sequenceNumber,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payload,
            Instant timestamp) {
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.actorId = actorId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payload = payload;
        this.timestamp = timestamp;
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
}
