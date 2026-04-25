package com.chronosincome.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClientResponse {

    private Long id;
    private String name;
    private String fiscalId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
