package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignedUrlResponse {
    String downloadUrl;
}
