package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CvResponse {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private LocalDateTime uploadedAt;
}
