package com.chronosincome.dto.request;

import com.chronosincome.entity.Project.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectStatusRequest {

    @NotNull(message = "Status é obrigatório")
    private ProjectStatus status;
}
