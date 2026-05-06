package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CvBuilderDraftPageResponse {
    private List<CvBuilderDraftResponse> items;
    private String nextCursor;
}
