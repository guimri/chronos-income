package com.chronosincome.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeEntryStartRequest {

    // Permite iniciar em data/hora específica ou usa o momento atual
    private LocalDate entryDate;
    private LocalTime startTime;

    private String description;

    @NotNull(message = "Projeto é obrigatório")
    private Long projectId;

    // Quando true, o entry é criado pausado (padrão do calendário)
    @Builder.Default
    private boolean startPaused = false;
}
