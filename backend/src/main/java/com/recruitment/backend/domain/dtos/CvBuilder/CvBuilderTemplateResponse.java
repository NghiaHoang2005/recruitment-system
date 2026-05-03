package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CvBuilderTemplateResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String previewImageUrl;
    private Integer displayOrder;
}
