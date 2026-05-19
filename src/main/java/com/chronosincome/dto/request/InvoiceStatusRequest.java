package com.chronosincome.dto.request;

import com.chronosincome.entity.Invoice.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InvoiceStatusRequest {

    @NotNull(message = "Status é obrigatório")
    private InvoiceStatus status;
}