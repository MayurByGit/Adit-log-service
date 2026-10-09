package com.example.audit.repository;

import com.example.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findAllByOrderBySequenceNumberAsc();

    List<AuditEvent> findByTimestampBeforeAndArchivedFalse(
            Instant cutoff
    );

    @Query("""
        select e from AuditEvent e
        where (:actorId is null or e.actorId = :actorId)
          and (:resourceType is null or e.resourceType = :resourceType)
          and (:resourceId is null or e.resourceId = :resourceId)
          and (:eventType is null or e.eventType = :eventType)
          and (:fromTime is null or e.timestamp >= :fromTime)
          and (:toTime is null or e.timestamp <= :toTime)
        """)
    Page<AuditEvent> search(
            @Param("actorId") String actorId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("eventType") String eventType,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );
}