package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCvBuilderDraftRequest {
    private String title;
    private String contentJson;
}
