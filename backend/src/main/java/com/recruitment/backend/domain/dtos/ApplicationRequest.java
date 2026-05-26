package com.recruitment.backend.domain.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class ApplicationRequest {
    private UUID jobId;
    private UUID cvId;
    private String coverLetter;
}
