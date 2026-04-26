package com.recruitment.backend.domain.dtos.Cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvItemResponse {
    private UUID id;
    private String cvName;
    private LocalDateTime uploadedAt;
    private boolean isDefault;
}
