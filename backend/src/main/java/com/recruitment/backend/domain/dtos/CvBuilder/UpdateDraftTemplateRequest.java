package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateDraftTemplateRequest {
    private UUID templateId;
}
