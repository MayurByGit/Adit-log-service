
package com.example.audit.controller;

import com.example.audit.dto.AuditExportResponse;
import com.example.audit.service.AuditArchiveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestController
@RequestMapping("/audit/archive")
public class AuditArchiveController {

    private final AuditArchiveService archiveService;

    public AuditArchiveController(AuditArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/retention")
    public ResponseEntity<Map<String, Object>> archiveByRetention(
            @RequestParam int retentionDays) {

        int archivedCount =
                archiveService.archiveOlderThanDays(retentionDays);

        return ResponseEntity.ok(Map.of(
                "retentionDays", retentionDays,
                "archivedCount", archivedCount,
                "message", "Retention operation completed"
        ));
    }

    @GetMapping("/export")
    public AuditExportResponse export(
            @RequestParam(defaultValue = "false")
                    boolean redact) {

        return archiveService.exportEvents(redact);
    }
}

@RestControllerAdvice(assignableTypes = AuditArchiveController.class)
class AuditArchiveExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(
            IllegalArgumentException exception) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }
}
