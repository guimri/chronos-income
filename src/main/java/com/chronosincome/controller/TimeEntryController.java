package com.chronosincome.controller;

import com.chronosincome.dto.request.*;
import com.chronosincome.dto.response.TimeEntryResponse;
import com.chronosincome.service.TimeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    // Entrada manual
    @PostMapping("/manual")
    public ResponseEntity<TimeEntryResponse> createManual(
            @Valid @RequestBody TimeEntryManualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeEntryService.createManual(request));
    }

    // Temporizador
    @PostMapping("/timer/start")
    public ResponseEntity<TimeEntryResponse> startTimer(
            @Valid @RequestBody TimeEntryStartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeEntryService.startTimer(request));
    }

    @PatchMapping("/timer/{id}/pause")
    public ResponseEntity<TimeEntryResponse> pauseTimer(@PathVariable Long id) {
        return ResponseEntity.ok(timeEntryService.pauseTimer(id));
    }

    @PatchMapping("/timer/{id}/resume")
    public ResponseEntity<TimeEntryResponse> resumeTimer(@PathVariable Long id) {
        return ResponseEntity.ok(timeEntryService.resumeTimer(id));
    }

    @PatchMapping("/timer/{id}/stop")
    public ResponseEntity<TimeEntryResponse> stopTimer(@PathVariable Long id) {
        return ResponseEntity.ok(timeEntryService.stopTimer(id));
    }

    // Restaura o estado do temporizador ao recarregar o React
    @GetMapping("/timer/active")
    public ResponseEntity<TimeEntryResponse> findActiveTimer() {
        TimeEntryResponse active = timeEntryService.findActiveTimer();
        return active != null
                ? ResponseEntity.ok(active)
                : ResponseEntity.noContent().build();
    }

    // Consultas
    @GetMapping
    public ResponseEntity<List<TimeEntryResponse>> findAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start != null && end != null) {
            return ResponseEntity.ok(timeEntryService.findByPeriod(start, end));
        }
        return ResponseEntity.ok(timeEntryService.findAll());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TimeEntryResponse>> findByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(timeEntryService.findByProject(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeEntryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(timeEntryService.findById(id));
    }

    // Edição e exclusão
    @PutMapping("/{id}")
    public ResponseEntity<TimeEntryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TimeEntryUpdateRequest request) {
        return ResponseEntity.ok(timeEntryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
