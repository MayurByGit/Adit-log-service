package com.example.audit.controller;

import com.example.audit.dto.ComplianceReportResponse;
import com.example.audit.service.ComplianceReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/audit/compliance")
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;

    public ComplianceReportController(
            ComplianceReportService complianceReportService) {
        this.complianceReportService = complianceReportService;
    }

    @GetMapping("/report")
    public ComplianceReportResponse generateReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant startTime,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant endTime,

            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String eventType) {

        return complianceReportService.generateReport(
                startTime, endTime, actorId, eventType);
    }
}

@RestControllerAdvice(assignableTypes = ComplianceReportController.class)
class ComplianceReportExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameter(
            NullPointerException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage() == null
                        ? "Required reporting parameter is missing"
                        : exception.getMessage()));
    }
}