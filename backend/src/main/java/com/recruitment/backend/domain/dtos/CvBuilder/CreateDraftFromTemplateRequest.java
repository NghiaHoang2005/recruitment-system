package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateDraftFromTemplateRequest {
    private UUID templateId;
    private String title;
    private String profileSeed;
}
