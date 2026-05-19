package com.chronosincome.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "Projeto é obrigatório")
    private Long projectId;

    @NotNull(message = "Data de início do período é obrigatória")
    private LocalDate periodStart;

    @NotNull(message = "Data de fim do período é obrigatória")
    private LocalDate periodEnd;
}
