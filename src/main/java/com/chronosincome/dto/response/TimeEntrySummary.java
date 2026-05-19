package com.chronosincome.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeEntrySummary {

    private Long id;
    private LocalDate entryDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String duration;
    private String description;
}
