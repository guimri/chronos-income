package com.chronosincome.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Cor é obrigatória")
    private String color;

    // RN11 — valor hora deve ser positivo
    @NotNull(message = "Valor por hora é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor por hora deve ser maior que zero")
    private BigDecimal hourlyRate;

    private String description;

    private LocalDate startDate;

    @NotNull(message = "Cliente é obrigatório")
    private Long clientId;
}
