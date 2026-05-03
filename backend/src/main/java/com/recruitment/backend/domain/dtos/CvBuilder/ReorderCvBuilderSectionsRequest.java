package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReorderCvBuilderSectionsRequest {
    private List<String> sectionIds;
}
