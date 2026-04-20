package com.recruitment.backend.domain.dtos.Cv;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CvReviewRequest {
    private UUID jobId;
}
