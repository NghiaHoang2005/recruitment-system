package com.recruitment.backend.domain.dtos.Cv;

import lombok.Data;

@Data
public class CvUploadCompleteRequest {
    private String cvName;
    private String publicId;
}
