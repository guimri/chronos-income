package com.chronosincome.controller;

import com.chronosincome.dto.request.InvoiceRequest;
import com.chronosincome.dto.request.InvoiceStatusRequest;
import com.chronosincome.dto.response.InvoiceResponse;
import com.chronosincome.entity.Invoice.InvoiceStatus;
import com.chronosincome.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> findAll(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long projectId) {

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