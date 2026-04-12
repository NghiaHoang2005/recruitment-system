package com.recruitment.backend.domain.dtos.Cv;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CvUploadRequest {
    private MultipartFile file;
}
