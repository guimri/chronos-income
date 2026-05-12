package com.chronosincome.dto.response;

import com.chronosincome.entity.TimeEntry.EntryType;
import com.chronosincome.entity.TimeEntry.TimerStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeEntryResponse {

    private Long id;
    private LocalDate entryDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Duração formatada HH:MM:SS para o React exibir diretamente
    private String duration;

    private EntryType type;
    private TimerStatus timerStatus;
    private String description;
    private Boolean invoiced;

    private Long projectId;
    private String projectName;
    private String projectColor;

    private Long clientId;
    private String clientName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
