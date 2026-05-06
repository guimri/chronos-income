package com.chronosincome.dto.response;

import com.chronosincome.entity.Project.ProjectStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String color;
    private BigDecimal hourlyRate;
    private String description;
    private LocalDate startDate;
    private ProjectStatus status;

    // Dados resumidos do cliente para o React exibir sem precisar de outra chamada
    private Long clientId;
    private String clientName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}