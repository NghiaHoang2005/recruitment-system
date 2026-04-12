package com.recruitment.backend.domain.dtos.Cv;

import com.recruitment.backend.domain.entities.Cv.CvStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionStatusResponse {
    private UUID cvId;
    private CvStatus status;
    private String parsedData;
    private String errorMessage;
}
