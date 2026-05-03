package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCustomSectionRequest {
    private String sectionType;
    private String sectionTitle;
    private String dataJson;
    private Integer insertAt;
}
