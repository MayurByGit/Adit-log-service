package com.example.audit.controller;

import com.example.audit.dto.AuditExportResponse;
import com.example.audit.service.AuditArchiveService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/audit/archive")
public class AuditArchiveController {

    private final AuditArchiveService archiveService;

    public AuditArchiveController(
            AuditArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping("/export")
    public AuditExportResponse exportEvents(
            @RequestParam(defaultValue = "false")
                    boolean redact) {

        return archiveService.exportEvents(redact);
    }

    @PostMapping("/retention")
    public Map<String, Object> applyRetention(
            @RequestParam(defaultValue = "365")
                    int retentionDays) {

        try {
            int archivedCount =
                    archiveService.archiveOlderThanDays(retentionDays);

            return Map.of(
                    "success", true,
                    "retentionDays", retentionDays,
                    "archivedCount", archivedCount,
                    "message", "Retention processing completed"
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }
}