package com.chronosincome.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeEntryUpdateRequest {

    @NotNull(message = "Data é obrigatória")
    private LocalDate entryDate;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de fim é obrigatório")
    private LocalTime endTime;

    private String description;

    @NotNull(message = "Projeto é obrigatório")
    private Long projectId;
}
