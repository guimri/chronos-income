package com.chronosincome.controller;

import com.chronosincome.dto.request.InvoiceRequest;
import com.chronosincome.dto.request.InvoiceStatusRequest;
import com.chronosincome.dto.response.InvoiceResponse;
import com.chronosincome.entity.Invoice.InvoiceStatus;
import com.chronosincome.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(request));
    }

    /**
     * GET /api/invoices
     * Parâmetros opcionais combinados:
     *   ?status=PAID
     *   ?start=2026-05-01&end=2026-05-31
     *   ?status=PAID&start=2026-05-01&end=2026-05-31
     *   ?clientId=1
     *   ?projectId=2
     */
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> findAll(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        // Filtro por período + status
        if (start != null && end != null && status != null) {
            return ResponseEntity.ok(invoiceService.findAllByStatusAndPeriod(status, start, end));
        }
        // Filtro só por período
        if (start != null && end != null) {
            return ResponseEntity.ok(invoiceService.findAllByPeriod(start, end));
        }
        // Filtros originais
        if (status != null) {
            return ResponseEntity.ok(invoiceService.findAllByStatus(status));
        }
        if (clientId != null) {
            return ResponseEntity.ok(invoiceService.findAllByClient(clientId));
        }
        if (projectId != null) {
            return ResponseEntity.ok(invoiceService.findAllByProject(projectId));
        }
        return ResponseEntity.ok(invoiceService.findAll());
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<List<InvoiceResponse>> findByClient(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(invoiceService.findAllByClient(clientId));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<InvoiceResponse>> findByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(invoiceService.findAllByProject(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceStatusRequest request) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok("Invoice deletado com sucesso");
    }
}