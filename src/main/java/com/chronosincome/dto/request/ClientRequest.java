package com.chronosincome.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClientRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "CNPJ ou EIN é obrigatório")
    private String fiscalId;

    private String description;
}