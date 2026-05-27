package com.chronosincome.controller;

import com.chronosincome.dto.request.ProjectRequest;
import com.chronosincome.dto.request.ProjectStatusRequest;
import com.chronosincome.dto.response.ProjectResponse;
import com.chronosincome.entity.Project.ProjectStatus;
import com.chronosincome.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> findAll(
            @RequestParam(required = false) ProjectStatus status) {
        if (status != null) {
            return ResponseEntity.ok(projectService.findAllByStatus(status));
        }
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ProjectResponse>> findAllByClient(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(projectService.findAllByClient(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStatusRequest request) {
        return ResponseEntity.ok(projectService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok("Projeto deletado com sucesso");
    }
}
