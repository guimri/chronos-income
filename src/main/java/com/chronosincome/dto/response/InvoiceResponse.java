package com.chronosincome.dto.response;

import com.chronosincome.entity.Invoice.InvoiceStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // Duração total formatada HH:MM:SS
    private String totalDuration;

    private BigDecimal hourlyRate;
    private BigDecimal totalAmount;
    private InvoiceStatus status;

    private Long clientId;
    private String clientName;
    private String clientFiscalId;

    private Long projectId;
    private String projectName;

    // Registros de horas que compõem a fatura
    private List<TimeEntrySummary> timeEntries;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}